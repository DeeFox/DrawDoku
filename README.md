# Draw! - Protokollbeschreibung

#### Vorwort
In diesem Dokument werden alle Pakete, die zwischen einem Draw!-Client und dem Draw!-Server verschickt werden aufgelistet und detailliert beschrieben. Sollte es zu Unklarheiten kommen, schreibt bitte eine E-Mail an nils.sonemann@gmail.com.

Der Draw!-Server ist unter der URL: websocket://foo.bar:8080/ zu erreichen. Sämtliche Pakete werden als JSON-Datenstrukturen versendet.

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
{"action":"gameslist", 
"games":[
    {"gameid":56, "opponent":"VanGogh", "gamestate":"yourturn_draw", "streak":5, "gamepoints":800, "lastactiontime":"1466351919"},
    {"gameid":32, "opponent":"Dali", "gamestate":"opponentturn_guess", "streak":2, "gamepoints":300, "lastactiontime":"1466351422"}
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
{"action":"doturn", "gameid":"99"}
```

Durch Senden dieses Paketes signalisiert der Client dem Server, dass er einen Spielzug durchführen möchte. Welcher Spielzug an der Reihe ist, wird durch den Server entschieden und dementsprechend kommt ein bestimmtes Antwort-Paket zurück.

  - **Server -> Client**
```json
{"action":"chooseword", "words":{
    "easy":{"word":"Haus", "points":100},
    "medium":{"word":"Gitarre", "points":200},
    "hard":{"word":"Karnaugh-Diagramm", "points":400}
}}
```
Ist der aktuell durchzuführende Spielzug das Malen eines Bildes für den Mitspieler und der Spieler hat noch kein zu malendes Wort gewählt, sender der Server dem Client eine Liste von Worten zu, aus denen er eines wählen kann. Jedes Wort ist einer Schwierigkeitsstufe zugeordnert, welche jeweils unterschiedlich viele Punkte einbringen. Der Spieler der das gemalte Bild errät, erhält 75% der Punkte, derjenige der das korrekt erratene Bild gemalt hat, erhält 25% der Punkte.

_oder_
```json
{"action":"readyfordrawing"}
```
Ist der aktuell durchzuführende Spielzug das Malen eines Bildes für den Mitspieler und der Spieler hat bereits einen Begriff gewählt, sendet der Server dieses Paket zum Client. Der Client sollte daraufhin die Ansicht zur Malen-UI wechseln und den Spieler auffordern das gewählte Wort zu malen.

---
##### Durchführen eines Spielzuges #####
