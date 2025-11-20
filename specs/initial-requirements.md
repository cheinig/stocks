# 1. Was soll erstellt werden
Es soll eine Web Applikation entstehen, die eine Übersicht von Aktien und ETF bereitstellt, und deren Zuordnung zu Ländern. Die Applikation soll dazu die Möglichkeit bieten Dateien hostzuladen, in denen die Aufschlüsselung von ETF zu den beinhalteten Assets aufgelistet sind. Diese werden dann gespeichert und die Enthaltenen Werte können aufgelistet werden. Außerdem kann ich ein Portfolio zusammen stellen, in dem ich vorgebe, wie viele Anteile ich an einer Aktie oder einem ETF habe und das System erreichnet dazu automatisch wie viele Anteile ich an einer Aktie in Summe habe, sowie meine Länderallokation

# 2. Use Cases
- Dashboard mit dem aktuellen Portfolio (Aktien und ETFs), sowie den Top 20 Aktienwerten unter berücksichtigung der aufgeschlüsselten ETFs
- CRUD Funktionalität um einen Basis Aktienwert anzulegen und zu verwalten (Name, ISIN, Land, Branche)
- CRUD Funktionalität um einen ETF anzulegen und zu verwalten (Name, ISIN, Importer). Der Importer ist eine im System hinterlegte Funktion, die CSV oder Excel Dateien entgegen nimmt und damit die Aktienallokation des ETFs ermitteln kann.
- Upload einer neuen Allokationsdatei für einen ETF um die Zusammensetzung zu aktualisieren.
- CRUD Funktionalität um das eigene Portfolio zu verwalten. Es können vorher definierte Aktien und ETFs benutzt werden.

# 3. Technische Anforderungen
siehe Datei 3-technical-requirements.md
