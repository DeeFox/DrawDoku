# Draw! - Protokollbeschreibung
_Stand: 09.07.2016 (4:00 Uhr)_
Die Server-Datenbank wurde wegen einer Anbindungs-Optimierung geleert!_

_Stand: 02.07.2016_
Antwort-Paket für den Drawdata-Request wurde eingepflegt.

_Stand: 30.06.2016_
Alle Funktionen wurden freigeschaltet. Die Server-Datenbank wurde geleert. Die Dokumentation wurde fertiggestellt.

_Stand: 28.06.2016_
An diversen Stellen _gameid_-Felder eingepflegt. Alle Schritte für das Erstellen von Spielen und das Übermitteln von Draw-Daten dokumentiert. **Die Server-Datenbank wird in Zukunft regelmäßig geleert werden, bitte passt eure App darauf an, dass die zwischengespeicherten _auth_-Daten auch wieder gelöscht werden können, wenn der _auth_-Vorgang fehlschlägt ;-)**

_Stand: 21.06.2016_
Gegner-ID, Gegner-Score und eigener Score werden in der Gameslist mit übertragen.

_Stand: 20.06.2016_

#### Vorwort
In diesem Dokument werden alle Pakete, die zwischen einem Draw!-Client und dem Draw!-Server verschickt werden aufgelistet und detailliert beschrieben. 

Sollte es zu Unklarheiten mit dem Protokoll kommen, schreibt bitte eine E-Mail an nils.sonemann@gmail.com.

Bei Fragen zu Android und Android Studio wendet euch bitte vorrangig an jp.lange3@gmail.com.

Der Draw!-Server ist unter der URL: ws://draw.dfox.eu:8080/ zu erreichen. Sämtliche Pakete werden als JSON-Datenstrukturen versendet.

Eine unter Android getestete Implementierung von Websockets lässt sich hier finden:
https://github.com/TooTallNate/Java-WebSocket
Die Einbindung in die build.gradle Datei kann dabei wie folgt aussehen:
```
compile 'org.java-websocket:Java-WebSocket:1.3.0'
```

#### Protokoll
Grundsätzlich sehen alle Anfragen an und alle Antworten vom Server ähnlich aus. Die JSON-Pakete enthalten immer ein Feld _action_, welches Auskunft darüber gibt, wie das Paket zu interpretieren ist. Der Client muss sich also merken, in welchem Kontext er sich befindet und dann dementsprechend auf die Pakete reagieren.
Im Normalfall sollte der Client vom Server die hier im Dokument beschriebenen Antworten zurückbekommen. Es kann jedoch auf Grund einer Vielzahl von Gründen zu Fehlern bei der Verarbeitung kommen, weshalb es eine Error-Nachricht gibt:
  - **Server -> Client**
```json
{"action":"error", "errmsg":"You did this wrong!"}
```
Diese Fehlermeldung kann auf Grund von falscher Formatierung der Eingabedaten oder ungültigen Anfragen an den Client geschickt werden. Grundsätzlich sollte die Nachricht (_errmsg_) immer auf der Konsole ausgegeben werden und natürlich auch abhängig vom Kontext der Anwendung darauf reagiert werden.
**Sollte die Nachricht im Feld _errmsg_ mit _Internal Server Exception!_ beginnen, so ist die Chance, dass der Client eine falsche Anfrage geschickt hat, relativ gering. Der Admin wird über diese Art von Fehlermeldungen informiert und versucht natürlich so schnell wie möglich eine Lösung dafür zu finden.**


---
##### Registrierung eines Benutzeraccounts
  - **Client -> Server**
```json
{"action":"register", "name":"Picasso"}
```
Um einen Benutzeraccount zu registrieren, ist lediglich ein Name nötig. Dieser wird im Feld _name_ übergeben. Es werden nur Namen bestehend aus Buchstaben und Zahlen akzeptiert. Andere Namen können abgewiesen werden oder alle ungültigen Zeichen serverseitig entfernt werden.
  - **Server -> Client**
```json
{"action":"registerdata", "username":"Picasso", "userid":123456, "usersecret":"..."}
```
Im Erfolgsfall erhält der Client eine Nachricht mit der _action_ _"registerdata"_. Zusätzlich enthalten in der Nachricht sind der Benutzername nach Prüfung und ggf. Anpassung durch den Server, sowie eine vom Server zugewiesene Benutzer-ID und ein geheimer String (_usersecret_), welcher zur Authentifizierung bei folgenden Interaktionen benötigt wird. Der Client sollte sich den geprüften Benutzernamen, die Benutzer-ID und den geheimen String persistent abspeichern. Die Benutzer-ID und das Secret werden bei zukünftigen Verbindungs-Versuchen immer benötigt. Die Benutzer-ID sollte in der UI prominent angezeigt werden, da über diese ID neue Spiele von anderen Clienten angefangen werden können.

---
##### Authentifizierung #####
  - **Client -> Server**
```json
{"action":"auth", "userid":123456, "usersecret":"..."}
```
Direkt nach erfolgreicher Registrierung am Server und bei jedem neuen Verbindungsaufbau zum Server muss dieses Paket an den Server geschickt werden, damit dieser weiß, mit welchem Client er verbunden ist und ob dieser für die Aktionen, welcher er ausführen möchte, berechtigt ist.
In den Feldern _userid_ und _usersecret_ müssen die gespeicherten Daten aus der Antwort des _register_-Pakets gesendet werden.
  - **Server -> Client**
```json
{"action":"authenticated", "username":"Picasso"}
```
Wurden korrekte Daten übermittelt, teilt der Server dem Client mit, dass dieser nun authentifiziert ist. Zusätzlich wird erneut der Benutzername des Clients übermittelt, falls dieser den Namen nicht zwischengespeichert hat. Im Fehlerfall wird ein _error_-Paket an den Client geschickt, dieser sollte also entsprechend reagieren können.

---
##### Anfragen der Spiele-Übersicht #####
  - **Client -> Server**
```json
{"action":"games"}
```
Sobald der Client authentifiziert ist, kann eine Übersicht aller offenen Spiele angefragt werden. Dazu sendet der Client dieses einfache Paket an den Server.
  - **Server -> Client**
```json
{"action":"gameslist", "score":1250,
"games":[
    {"gameid":56, "opponent":"VanGogh", "opponentid":223322, "opponentscore":3300, "gamestate":"yourturn_draw", "streak":5, "gamepoints":800, "lastactiontime":"1466351919"},
    {"gameid":32, "opponent":"Dali", "opponentid":341234, "opponentscore":1000, "gamestate":"opponentturn_guess", "streak":2, "gamepoints":300, "lastactiontime":"1466351422"}
]}
```
Der Server sendet daraufhin eine Liste aller offenen Spiele inklusive einiger Zusatzinformationen an den Client. Dieses Paket kann auch ohne vorheriges Anfragen durch den Client gesendet werden, wenn sich bei den eigenen Spielen Änderungen ergeben. So bekommt der Client eine aktualisierte Liste seiner Spiele wenn z.B. ein anderer Spieler ein Bild erraten hat und nun der Client mit Raten dran ist.

Die Spiele werden in dem Feld _games_ als Array übergeben. Jedes einzelne Element spiegelt dabei ein Spiel wieder. Die übertragenen Daten sind wie folgt:
  - _gameid_ ist eine interne Identifikationsnummer, die benötigt wird, um Züge durchzuführen
  - _opponent_ ist der Name des Gegenspielers
  - _gamestate_ spiegelt den aktuellen Zustand des Spieles wieder. Die Werte _"yourturn_draw"_, _"yourturn_guess"_, _"opponentturn_draw"_ und _"opponentturn_guess"_ sind hierbei möglich. Bei den States, welche mit _opponentturn__ beginnen, soll dem Spieler eine entsprechende Meldung angezeigt werden, bei den States, welche mit _yourturn__ beginnen, besteht die Möglichkeit einen Spielzug durchzuführen.
  - _streak_ gibt Information darüber, wie viele erratene Zeichnungen der Client und Mitspieler in diesem Spiel bereits geschafft haben
  - _gamepoints_ zeigt an, wie viele Punkte in diesem Spiel schon erspielt worden sind
  - _lastactiontime_ ist der UNIX-Timestamp der letzten Aktion. Denkbar wäre eine Anzeige, wann der letzte Spielzug durchgeführt wurde und eine Sortierung anhand von diesem Timestamp

---
##### Starten eines neuen Spiels #####
  - **Client -> Server**
```json
{"action":"startnew", "mode":"random"}
```
_oder_
```json
{"action":"startnew", "mode":"id", "opponentid":456789}
```

Um ein neues Spiel zu starten hat der Client zwei Möglichkeiten zur Auswahl. Zum einen kann ein Spiel mit einem zufälligen Gegner gestartet werden. Dazu muss in dem Feld _mode_ der Wert _"random"_ gesendet werden. Die andere Möglichkeit besteht darin, ein Spiel mit einem bestimmten Benutzer, dessen Benutzer-ID man kennt, zu starten. Dazu muss das Feld _mode_ mit dem Wert _"id"_ und das Feld _opponentid_ mit einer gültigen Benutzer-ID ausgefüllt werden. Sollte die ID nicht gültig sein, oder kein gültiger _mode_ ausgewählt worden sein, wird eine _error_-Nachricht zurückgeschickt. Im Erfolgsfall wird eine aktualisierte Liste der eigenen Spiele mit Hilfe eines _gameslist_-Pakets an den Clienten gesendet.

---
##### Durchführen eines Spielzuges #####
_Mit Spielzug ist hierbei das Malen eines Bildes oder das Erraten eines Bildes vom Mitspieler gemeint._
  - **Client -> Server**
```json
{"action":"doturn", "gameid":99}
```

Durch Senden dieses Paketes signalisiert der Client dem Server, dass er einen Spielzug durchführen möchte. Welcher Spielzug an der Reihe ist, wird durch den Server entschieden und dementsprechend kommt ein bestimmtes Antwort-Paket zurück.

  - **Server -> Client**
```json
{"action":"chooseword", "gameid":159, "words":{
    "easy":{"word":"Haus", "points":100},
    "medium":{"word":"Gitarre", "points":200},
    "hard":{"word":"Karnaugh-Diagramm", "points":400}
}}
```
Ist der aktuell durchzuführende Spielzug das Malen eines Bildes für den Mitspieler und der Spieler hat noch kein zu malendes Wort gewählt, sender der Server dem Client eine Liste von Worten zu, aus denen er eines wählen kann. Jedes Wort ist einer Schwierigkeitsstufe zugeordnert, welche jeweils unterschiedlich viele Punkte einbringen. Der Spieler der das gemalte Bild errät, erhält 75% der Punkte, derjenige der das korrekt erratene Bild gemalt hat, erhält 25% der Punkte. Die _gameid_ des Spiels wird noch einmal mitgesendet. Die Antwort auf dieses Paket, um dem Server mitzuteilen, für welches Wort der Spieler sich entschieden hat, findet sich unter dem Punkt _Zu malendes Wort festlegen_.

_oder_
  - **Server -> Client**
```json
{"action":"readyfordrawing", "gameid":55, "word":"GITARRE"}
```
Ist der aktuell durchzuführende Spielzug das Malen eines Bildes für den Mitspieler und der Spieler hat bereits einen Begriff gewählt, sendet der Server dieses Paket zum Client. Das gewählte Wort und die Spiel-ID werden noch einmal mitgesendet. Der Client sollte daraufhin die Ansicht zur Malen-UI wechseln und den Spieler auffordern das gewählte Wort zu malen. Ist der Spieler mit dem Malen fertig, so soll das Gemalte als Paket an den Server gesendet werden. Die Einzelheiten dazu sind unter dem Punkt _Abschicken eines gemalten Bilds_ zu finden.

_oder_

  - **Server -> Client**
```json
{"action":"readyfordrawdatarequest", "gameid":55, "chunks":5}
```
Ist der aktuell durchzuführende Spielzug das Erraten eines Bildes, so bestätigt der Server dies dem Clienten und signalisiert die Bereitschaft auf Anfragen zu den Bild-Daten zu reagieren. Dem Clienten wird so ein Zeitpunkt gegeben, an dem die UI umgeschaltet werden kann. Im Feld _chunks_ wird die Anzahl der Blöcke mit Bilddaten signalisiert, die der Client abfragen kann. Dem Client ist dabei überlassen, ob er zu Beginn alle Blöcke abruft oder erst nach und nach, nachdem ein Block gezeichnet wurde, den nächsten Block abruft. Bei sukzessivem Abruf der Daten kann der Benutzer jederzeit das Zeichnen des Bildes unterbrechen und vorzeitig das Wort erraten. Je früher er das Bild errät, desto mehr Punkte erhalten beide Spieler. Bei Erraten des Wortes nach Abruf aller Daten werden 50% der Punkte ausgeschüttet. Die Nachricht, die der Client an den Server senden muss, um vorzeitig das Wort zu erraten ist unter _(Vorzeitiges) Erraten des Bildes_ nachzulesen. Die Nachrichten, die der Client an den Server senden muss, um die Daten-Blöcke abzurufen, sind unter _Draw-Datenblöcke abrufen_ nachzulesen.

_oder_
  - **Server -> Client**
```json
{"action":"guessword", "gameid":55, "chars":"GIRHRAMANMAKUAGDSDER", "blanks":"########-########", "points":320}
```
Ist der aktuell durchzuführende Spielzug das Erraten eines Bildes und der Client hat bereits das zu erratene Bild abgerufen, sendet der Server dem Clienten die Anweisung das Wort zu erraten. Dafür steht eine Maske im Feld _blanks_ bereit. Leerzeichen und Sonderzeichen werden mitgesendet, alle # stehen für einen Buchstaben, der vom Benutzer eingegeben werden muss. Zum Ausfüllen der Leerstellen stehen die 20 Buchstaben aus dem Feld _chars_ bereit. Jeder dieser Buchstaben darf nur einmalig an einer Stelle eingesetzt werden.
Beispiel-UI:
![Guessing-UI](https://raw.githubusercontent.com/DeeFox/DrawDoku/master/keys_ui.png "Guessing-UI")

---
##### Zu malendes Wort festlegen #####
  - **Client -> Server**
```json
{"action":"wordchosen", "gameid":55, "word":"medium"}
```
Hat der Client die Liste mit zur Verfügung stehenden Wörtern erhalten und der Spieler sich für ein Wort entschieden, so kann mit dieser Nachricht das gewählte Wort gespeichert werden. Die Game-Id muss mit übergeben werden. Das ausgewählte Wort soll nicht direkt, sondern nur der Schwierigkeits-Identifier, übertragen werden. Mögliche Werte sind also _"easy"_, _"medium"_ und _"hard"_. Hat der Server diese Nachricht akzeptiert wird eine _"readyfordrawing"_-Nachricht wie im Punkt _Durchführen eines Spielzuges_ beschrieben an den Client geschickt.

---
##### Draw-Datenblöcke abrufen #####
  - **Client -> Server**
```json
{"action":"getdrawdata", "gameid":55, "chunk":0}
```
Nachdem der Client vom Server die Nachricht _"readyfordrawdatarequest"_ erhalten hat, kann der erste Daten-Block mit _chunk_-ID Null abgerufen werden. Es folgt eine Antwort des Servers im Format, welches unter _Abschicken eines gemalten Bilds_ beschrieben wird. Nun soll der Client die empfangenen Daten wiedergeben. Wurden alle empfangenen Daten gezeichnen, kann der Client einen weiteren Block mit inkrementierter _chunk_-ID abrufen und zeichnen. Der Spieler soll dabei jederzeit in der Lage sein, das Wiedergeben vorzeitig zu unterbrechen (siehe _(Vorzeitiges) Erraten des Bildes_). Die Gesamtanzahl an vorhandenen Chunks wird im Feld _chunks_ der _readyfordrawdatarequest_-Nachricht übermittelt.

  - *Server -> Client**
```json
{"action":"chunkeddrawdata", "gameid":55, "chunk":0, "totalchunks":20, "bgcolor":"#FF00FF", "data":[
  {"n":0, "type":"rline", "pts":[0.3, 0.5, 0.4, 0.8, 0.3, 0.3], "col":"#000000", "thick":0.2},
  {"n":1, "type":"bline", "pts":[[0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1],
                                 [0.2, 0.4, 0.67, 0.11, 0.2, 0.2, 0.2, 0.1]
  ], "col":"#000000", "thick":0.2}]
}
```
Die Antwort vom Server listet die Zeichen-Anweisungen des aktuellen Blocks im Format, wie unter _Abschicken eines gemalten Bilds_ beschrieben, auf. Die Hintergrundfarbe wird in jedem Chunk mitübertragen.

---
##### (Vorzeitiges) Erraten des Bildes #####
  - **Client -> Server**
```json
{"action":"stopdrawdata", "gameid": 55}
```
Hat der Client das Paket _readyfordrawdatarequest_ erhalten und lädt nach und nach die Bild-Daten während des Zeichnens herunter, kann dieses mit Hilfe dieses Pakets vorzeitig unterbrochen werden. Die könnte zum Beispiel gewollt sein, wenn der Spieler auf eine Art Stop-Button während des Playbacks drückt.
Als Antwort sendet der Server nun das _guessword_ - Paket, wie im Kapitel _Durchführen eines Spielzuges_ beschrieben.

---
##### Abschicken eines erratenen Worts #####
  - **Client -> Server**
```json
{"action":"guess", "gameid":55, "word":"GITARRE"}
```
Hat der Spieler ein Wort in die Eingabemaske eingegeben und möchte dieses zur Überprüfung abschicken, so muss dieses Paket an den Server gesendet werden.
Der Server antwortet daraufhin mit dem folgenden Paket, welches im Feld _result_ anzeigt, ob das Wort korrekt erraten wurde. Die beiden Möglichen Werte für das Feld sind "correct" oder "wrong". Die möglichen Punkte werden zur möglichen Erfolgs-Anzeige mitgesendet. Im Erfolgsfall wird nach dem gesendeten _guessresult_-Paket eine aktualisierte Games-Liste an beide Spieler gesendet.
  - **Server -> Client**
```json
{"action":"guessresult", "result":"correct", "points":"98"}
```

---
##### Abschicken eines gemalten Bilds #####
  - **Client -> Server**
```json
{"action":"drawingdata",
  "bgcolor":"#FF00FF",
  "gameid":55,
  "data":[
    {"n":0, "type":"rline", "pts":[0.3, 0.5, 0.4, 0.8, 0.3, 0.3], "col":"#000000", "thick":0.2},
    {"n":1, "type":"bline", "pts":[[0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1],
                                    [0.2, 0.4, 0.67, 0.11, 0.2, 0.2, 0.2, 0.1]
                                ], "col":"#000000", "thick":0.2},
    {"n":2, "type":"emoji", "c1":55357, "c2":56834, "pt":[0.4, 0.4], "size":0.4, "rot":0.55},
    {"n":3, "type":"shape", "shape":"circle", "pt":[0.1, 0.1], "r": 0.02, "col":"#000000"},
    {"n":4, "type":"shape", "shape":"rect", "pt":[0.1, 0.1], "dim":[0.2, 0.4], "rot":0.22, "col":"#000000"},
    {"n":5, "type":"shape", "shape":"tri", "pt":[0.2, 0.2], "dim":[0.2, 0.3], "rot":0.11, "col":"#012345"}
  ]
}
```
Hat der Spieler ein Wort gewählt und dieses fertig gemalt, so muss das gemalte an den Server übertragen werden. Das Format dafür ist beispielhaft in der oben angefügten JSON-Nachricht erkennbar. Allgemein muss die Game-ID und eine Hintergrundfarbe in Hex-Darstellung angegeben werden. Das Bild selbst besteht aus einem Array von Objekten, welche unterschiedliche Eigenschaften haben. Die Objekte müssen mit Hilfe des Felds _n_ aufsteigend sortiert sein, damit die Elemente beim Abspielen auf dem Gerät des Mitspielers in der korrekten Reihenfolge wiedergegeben werden können. Für Gerätekompatibilität müssen sämtliche Koordinaten-Angaben auf den Wertebereich [0, 1.0] heruntergerechnet werden. **Das Canvas beim Zeichnen und Anzeigen von Bildern soll dem Seitenverhältnis 4:3 entsprechen**. Das bedeutet, die Höhe des Bildes soll 4 Einheiten und die Breite des Bildes 3 Einheiten betragen, dementsprechend sind die Skalierungen von x- und y-Koordinaten nicht gleich!  Die Liniendicke soll ebenfalls im Wertebereich [0, 1.0] angegeben werden und sich auf die Breite des gezeichneten Bildes beziehen (Beispiel: 25px dicke Linie, 400px breites Bild -> Liniendicke: 0.0625).
  - _rline_ stellt einen normalen, ungeglätteten Pfad zwischen mehreren Punkten dar. Diese Punkte werden im Feld _pts_ immer abwechselnd X- und Y-Koordinate aufgelistet. Die Anzahl der so aufgelisteten Werte muss mindestens 4 betragen und durch 2 teilbar sein, damit ein Pfad zwischen den Punkten gezeichnet werden kann. Zusätzlich kann noch eine Linienfarbe in Hex-Darstellung und die Dicke der Linie im Wertebereich [0, 1.0] angegeben werden.
  - _bline_ stellt einen Pfad aus Bezier-Kurven dar. Die Teilabschnitte des Bezier-Pfades werden als einzelne kubische Bezier-Kurven im _pts_-Array abgelegt. Die Punkte sind dabei wie folgt angeordnet: [Punkt1_X, Punkt1_Y, Kontrollpunkt1_X, Kontrollpunkt1_Y, Kontrollpunkt2_X, Kontrollpunkt2_Y, Punkt2_X, Punkt2_Y]. Wie bei der normalen Linie können Farbe und Dicke angegeben werden.
  - _emoji_ ermöglicht es, einen Emoji auf der Zeichenfläche zu platzieren. Die Felder _c1_ und _c2_ bestimmen, welcher Emoji gezeichnet werden soll. Ein Beispiel, wie diese Integer-Werte berechnet werden können, findet sich [unter diesem Link.](https://github.com/DeeFox/DrawDoku/blob/master/EmojiTest.java). Die Angaben im Feld _pt_ stehen für die Position an der der Mittelpunkt des Emojis liegt, das Feld _size_ stellt die Größe und _rot_ die Rotation des Emoji dar. Der Wert für das Feld _size_ soll wie der _thick_-Parameter der Linien berechnet werden, der Wert für die Rotation _rot_ liegt ebenfalls im Wertebereich [0, 1.0] und stellt eine Rotation im Uhrzeigersinn von 0* -> 0.0 bis 360* -> 1.0 dar.
  - **Der Typ _shape_ wird aus zeitlichen Gründen nicht genutzt werden.**
