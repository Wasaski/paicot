"""Fluxo completo: abrir o Chrome, buscar "paicot", anotar os 3 primeiros links.

Executado via POST /api/v1/python/execute (PAICOT API).
Imprime o resultado no stdout.
"""
import paicot


def submitted():
    """Detecta se a busca Google foi executada (URL da omnibox mudou)."""
    r = paicot.accessibility.find("search?q=paicot")
    return len(r["elements"]) > 0


def collect_links():
    links = []
    for q in (".com", ".org", "www.", "http"):
        r = paicot.accessibility.find(q)
        for e in r["elements"]:
            t = (e["text"] or "").strip()
            if not t or t in links:
                continue
            if "Wasaski/paicot" in t or "actions/runs" in t:
                continue
            links.append(t)
    return links


# 1) Abrir o Chrome
print(paicot.apps.open("com.android.chrome"))
paicot.sleep(3)

# 2) Focar a omnibox e digitar
print(paicot.screen.tap(540, 110))
paicot.sleep(1)
print(paicot.keyboard.type_text("paicot"))
paicot.sleep(1)

# 3) Enviar a busca com multiplos caminhos
sent = False
f = paicot.accessibility.find("paicot")
row_y = None
for e in f["elements"]:
    if e["description"] == "paicot" and e["clickable"]:
        row_y = e["center_y"]
        break
print("linha da sugestao y:", row_y)

# caminho A: tocar o LADO DIREITO da linha (evita o filho de texto/icone)
if row_y:
    print(paicot.screen.tap(900, row_y))
    paicot.sleep(3)
    sent = submitted()
    print("enviado apos toque na linha:", sent)

# caminho B: tecla de busca do teclado
if not sent:
    for x, y in [(965, 2135), (985, 2130), (930, 2110)]:
        print(paicot.screen.tap(x, y))
        paicot.sleep(2)
        sent = submitted()
        print(f"enviado apos tecla ({x},{y}):", sent)
        if sent:
            break

# caminho C: tocar o centro da linha da sugestao
if not sent and row_y:
    print(paicot.screen.tap(540, row_y))
    paicot.sleep(3)
    sent = submitted()
    print("enviado apos toque no centro:", sent)

# 4) Coletar os links dos resultados com rolagem (ate 4 paginas) ate ter 3
seen = []
for _ in range(4):
    for link in collect_links():
        if link not in seen:
            seen.append(link)
    if len(seen) >= 3:
        break
    paicot.screen.swipe(540, 1800, 540, 700, 500)
    paicot.sleep(1)

# 5) Anotar os 3 primeiros links
print("=== 3 primeiros links ===")
for i, link in enumerate(seen[:3], 1):
    print(f"{i}. {link}")
print(f"total coletados: {len(seen)}")
if not seen:
    print("AVISO: nenhum link encontrado (verifique se a busca foi enviada)")
