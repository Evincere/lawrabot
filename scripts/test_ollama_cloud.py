import os
import time
import json
import requests
import argparse
from typing import List, Optional

# Configuración
BASE_URL = "https://ollama.com"
API_KEY_VAR = "OLLAMA_CLOUD_API_KEY"

class OllamaCloudTester:
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }

    def list_models(self) -> List[str]:
        """Obtiene la lista de modelos disponibles en Ollama Cloud."""
        print(f"\n🔍 Consultando modelos disponibles en {BASE_URL}...")
        try:
            response = requests.get(f"{BASE_URL}/api/tags", headers=self.headers)
            response.raise_for_status()
            models_data = response.json().get('models', [])
            return [m['name'] for m in models_data]
        except Exception as e:
            print(f"❌ Error al listar modelos: {e}")
            return []

    def run_chat_test(self, model: str, prompt: str, stream: bool = False):
        """Ejecuta una prueba de chat y mide métricas."""
        print(f"\n🚀 Iniciando prueba con modelo: {model}")
        print(f"📝 Prompt: \"{prompt}\"")
        
        payload = {
            "model": model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": stream
        }

        start_time = time.time()
        ttft = None  # Time To First Token
        full_response = ""

        try:
            if stream:
                response = requests.post(f"{BASE_URL}/api/chat", headers=self.headers, json=payload, stream=True)
                response.raise_for_status()
                
                print("📥 Respuesta (stream): ", end="", flush=True)
                for line in response.iter_lines():
                    if line:
                        if ttft is None:
                            ttft = time.time() - start_time
                        
                        chunk = json.loads(line)
                        content = chunk.get('message', {}).get('content', '')
                        print(content, end="", flush=True)
                        full_response += content
                        if chunk.get('done'):
                            break
                print("\n")
            else:
                response = requests.post(f"{BASE_URL}/api/chat", headers=self.headers, json=payload)
                response.raise_for_status()
                data = response.json()
                full_response = data.get('message', {}).get('content', '')
                print(f"📥 Respuesta: {full_response}")

            total_time = time.time() - start_time
            print(f"--- Métricas ---")
            print(f"⏱️ Tiempo total: {total_time:.2f}s")
            if ttft:
                print(f"⚡ Time To First Token (TTFT): {ttft:.2f}s")
            
            return full_response
        except Exception as e:
            print(f"\n❌ Error durante la prueba: {e}")
            return None

    def run_utility_test(self, model: str):
        """Prueba de utilidad específica para LawraBot (Extracción JSON)."""
        print(f"\n⚖️  Ejecutando prueba de utilidad (LawraBot Legal Extraction)...")
        
        legal_text = (
            "El Sr. Juan Pérez, DNI 12.345.678, con domicilio en Calle Falsa 123, "
            "solicita el divorcio unilateral de la Sra. María García, DNI 87.654.321. "
            "Contrajeron matrimonio el 15 de mayo de 2010 en la ciudad de Buenos Aires."
        )
        
        prompt = (
            f"Extrae los siguientes datos del texto en formato JSON estricto: "
            f"petitioner_name, petitioner_dni, respondent_name, respondent_dni, marriage_date. "
            f"Texto: {legal_text}"
        )
        
        result = self.run_chat_test(model, prompt, stream=False)
        if result:
            try:
                # Intentar parsear para validar que es JSON
                # Limpiar posibles markdowns
                clean_json = result.replace('```json', '').replace('```', '').strip()
                json.loads(clean_json)
                print("✅ Validación de Utilidad: El modelo generó un JSON válido.")
            except:
                print("⚠️  Advertencia: El modelo no generó un JSON puro/válido.")

def main():
    parser = argparse.ArgumentParser(description="Ollama Cloud Tester para LawraBot")
    parser.add_argument("--model", type=str, help="Nombre del modelo a probar")
    parser.add_argument("--stream", action="store_true", help="Habilitar streaming")
    args = parser.parse_args()

    from dotenv import load_dotenv
    load_dotenv()
    
    api_key_raw = os.environ.get(API_KEY_VAR)
    if not api_key_raw:
        print(f"❌ Error: No se encontró la variable de entorno {API_KEY_VAR}")
        print(f"Asegúrate de que esté definida en tu archivo .env")
        return

    # Manejar formato de lista [key1, key2, ...]
    if api_key_raw.startswith('[') and api_key_raw.endswith(']'):
        keys = [k.strip() for k in api_key_raw[1:-1].split(',')]
        api_key = keys[0]
        print(f"ℹ️  Detectada lista de keys. Usando la primera: {api_key[:8]}...")
    else:
        api_key = api_key_raw.strip()

    tester = OllamaCloudTester(api_key)

    # 1. Listar y seleccionar modelo
    available_models = tester.list_models()
    
    target_model = args.model
    if not target_model:
        if not available_models:
            print("⚠️ No se pudieron obtener modelos. Introduce el nombre manualmente.")
            target_model = input("📝 Nombre del modelo (ej. gpt-oss:120b): ").strip()
        else:
            print("\nModelos disponibles:")
            for idx, m in enumerate(available_models):
                print(f"  [{idx}] {m}")
            
            choice = input(f"\nSelecciona un número [0-{len(available_models)-1}] o escribe el nombre del modelo: ").strip()
            if choice.isdigit() and 0 <= int(choice) < len(available_models):
                target_model = available_models[int(choice)]
            else:
                target_model = choice

    if not target_model:
        print("❌ Operación cancelada.")
        return

    # 2. Pruebas
    print(f"\n--- Iniciando Suite de Pruebas para {target_model} ---")
    
    # Prueba 1: Chat Simple
    tester.run_chat_test(target_model, "Hola, ¿quién eres y qué modelo estás usando?", stream=args.stream)
    
    # Prueba 2: Utilidad LawraBot
    tester.run_utility_test(target_model)

if __name__ == "__main__":
    main()
