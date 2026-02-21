from flask import Flask, request, Response
from pymorphy3 import MorphAnalyzer
import threading
import time
import sys
import urllib.request
import urllib.error

app = Flask(__name__)
morph = MorphAnalyzer()

def start_flask():
    app.run(port=5000)

def test_service():
    time.sleep(2)
    try:
        url = "http://127.0.0.1:5000/lemma?word=Hi"
        with urllib.request.urlopen(url, timeout=5) as response:
            result = response.read().decode('utf-8')
            if result.lower() == "hi":
                print("Lemma service started successfully")
                return True
            else:
                print(f"Lemma service test failed: expected 'Hi', got '{result}'")
                sys.exit(1)
    except Exception as e:
        print(f"Lemma service startup test failed: {e}")
        sys.exit(1)

@app.route('/lemma', methods=['GET'])
def lemmatize():
    word = request.args.get('word')
    if not word:
        return "No word provided", 400
    lemma = morph.parse(word)[0].normal_form
    return lemma

if __name__ == '__main__':
    flask_thread = threading.Thread(target=start_flask, daemon=True)
    flask_thread.start()
    test_service()
    flask_thread.join()
