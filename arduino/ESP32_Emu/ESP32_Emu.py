import time
import json
import random
import threading
import websocket
from datetime import datetime

# --- CONFIG SERVIDOR LOCALHOST ---
SERVER_WS_URL = "ws://localhost:5000"
LIMITE_DESCONEXION = 3 * 60  # 3 minutes

# --- VARIABLES DE DATOS ---
ultima_temp = 25.0
ultima_hum = 50.0
ultima_temp_enviada = -999.0
ultima_hum_enviada = -999.0
estado_reles = [0, 0, 0, 0, 0, 0]

tiempo_sin_conexion = None
ws_global = None

def leer_sensores_simulados():
    """Emula las lecturas del SHT31 con variaciones graduales"""
    global ultima_temp, ultima_hum
    ultima_temp += random.uniform(-0.1, 0.1)
    ultima_hum += random.uniform(-0.2, 0.2)
    # Limitar rangos
    ultima_temp = max(10.0, min(40.0, ultima_temp))
    ultima_hum = max(20.0, min(80.0, ultima_hum))

def gestionar_reles():
    """Equivalente a gestionarReles()"""
    print(f" └─ [HARDWARE] Aplicando pines de relés físicos a: {estado_reles}")

def on_message(ws, message):
    global estado_reles
    try:
        data = json.loads(message)
        if "reles" in data:
            nuevo_estado = [1 if r else 0 for r in data["reles"]]
            if nuevo_estado != estado_reles:
                estado_reles = nuevo_estado
                print(f" └─ [WS IN] Config recibida y actualizada: {estado_reles}")
                gestionar_reles()
    except Exception as e:
        print(f"Error parseando WS: {e}")

def on_error(ws, error):
    pass

def on_close(ws, close_status_code, close_msg):
    global tiempo_sin_conexion, ws_global
    ws_global = None
    if tiempo_sin_conexion is None:
        tiempo_sin_conexion = time.time()
    print("--- [WS] Desconectado del servidor ---")

def on_open(ws):
    global tiempo_sin_conexion, ws_global
    ws_global = ws
    tiempo_sin_conexion = None
    print("--- [WS] Conectado al servidor ---")
    
def ws_thread_loop():
    while True:
        ws = websocket.WebSocketApp(SERVER_WS_URL,
                                  on_open=on_open,
                                  on_message=on_message,
                                  on_error=on_error,
                                  on_close=on_close)
        ws.run_forever()
        time.sleep(5) # Delay antes de reconectar

def main():
    global tiempo_sin_conexion, estado_reles
    global ultima_temp_enviada, ultima_hum_enviada
    
    print("Iniciando emulador de ESP32 Industrial (WebSocket) en localhost...")
    print("Enviando datos solo si cambian >= 0.1 (o cada 60s como Heartbeat).")
    
    # Iniciar el hilo de WebSockets en segundo plano
    wst = threading.Thread(target=ws_thread_loop)
    wst.daemon = True
    wst.start()
    
    ultimo_envio = 0
    
    while True:
        tiempo_actual = time.time()
        
        # Leemos sensores gradualmente en cada iteración
        leer_sensores_simulados()
        
        # Si hay conexion evaluamos si toca enviar
        if ws_global is not None:
            delta_temp = abs(ultima_temp - ultima_temp_enviada)
            delta_hum = abs(ultima_hum - ultima_hum_enviada)
            heartbeat = (tiempo_actual - ultimo_envio > 60.0)
            
            if heartbeat or delta_temp >= 0.1 or delta_hum >= 0.1:
                t_round = round(ultima_temp, 1)
                h_round = round(ultima_hum, 1)
                payload = {"temp": t_round, "hum": h_round}
                try:
                    ws_global.send(json.dumps(payload))
                    hora_actual = datetime.now().strftime('%H:%M:%S')
                    razon = "HEARTBEAT" if heartbeat and delta_temp < 0.1 and delta_hum < 0.1 else "CAMBIO"
                    print(f" └─ [{hora_actual}] [WS OUT - {razon}] Sensores enviados: {payload}")
                    ultimo_envio = tiempo_actual
                    ultima_temp_enviada = t_round
                    ultima_hum_enviada = h_round
                except Exception:
                    pass
        else:
            # Si no hay conexion, comprobar limite de deep sleep (3 mins)
            if tiempo_sin_conexion is not None:
                tiempo_caido = tiempo_actual - tiempo_sin_conexion
                if tiempo_caido > LIMITE_DESCONEXION:
                    print("\n[DEEP SLEEP] Han pasado 3 min sin conexión.")
                    print("Apagando relés por seguridad...")
                    estado_reles = [0, 0, 0, 0, 0, 0]
                    gestionar_reles()
                    print("Esperando 30 min (pausa completa)...")
                    time.sleep(30 * 60)
                    tiempo_sin_conexion = time.time() # Resetear timer al despertar
        
        time.sleep(1)

if __name__ == '__main__':
    main()