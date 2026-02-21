# Lemma Service Full Setup & Run (Windows)

This document contains everything needed to install Python, install packages, create the service file, and run the Flask-based lemmatization service using pymorphy3.

## 1. Install Python 3.11.x

Download: Python 3.11.9

Install to: `C:\Tools\Python311`

Verify installation:

```cmd
C:\Tools\Python311\python.exe --version
C:\Tools\Python311\python.exe -m pip --version
```

## 2. Install Required Packages

Run the following commands in your terminal to set up the environment:

```cmd
C:\Tools\Python311\python.exe -m pip install --upgrade pip setuptools
C:\Tools\Python311\python.exe -m pip install flask pymorphy3 pymorphy3-dicts-ru
```

## 3. Create the Flask Lemma Service

Folder: `C:\AnglerAsistant\Fine-tuning\tools\FlaskService`
File: `lemma_service.py`

```python
from flask import Flask, request, jsonify
from pymorphy3 import MorphAnalyzer

app = Flask(__name__)
morph = MorphAnalyzer()

def simple_lemma(word):
    try:
        # Use pymorphy3 to find the normal form
        return morph.parse(word)[0].normal_form
    except Exception:
        # fallback: remove last character if word is long enough
        return word[:-1] if len(word) > 3 else word

@app.route('/lemma', methods=['POST'])
def lemma_endpoint():
    data = request.get_json()
    word = data.get("word", "")
    lemma = simple_lemma(word)
    return jsonify({"word": word, "lemma": lemma})

if __name__ == "__main__":
    # Start the Flask server on port 5000
    app.run(port=5000)
```

## 4. Run the Flask Service

Execute this command to start the server:

```cmd
C:\Tools\Python311\python.exe C:\AnglerAsistant\Fine-tuning\tools\FlaskService\lemma_service.py
```

## 5. Test the Service

The service will run on http://127.0.0.1:5000/lemma. Test it using curl in a separate command prompt:

```cmd
curl -X POST -H "Content-Type: application/json" -d "{\"word\":\"карипка\"}" http://127.0.0.1:5000/lemma
```

Expected JSON response:

```json
{
  "word": "карипка",
  "lemma": "карипк"
}
```
