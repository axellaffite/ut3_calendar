# Calendrier UT3 (Anciennement EDT UT3) ![Logo](https://github.com/axellaffite/ut3_calendar/blob/master/logo/ic_launcher/res/mipmap-mdpi/ic_launcher.png?raw=true)

Cette application permet de consulter le calendrier Celcat de l'université Paul Sabatier directement sur votre téléphone.  
Disponible pour Android --> [ici](https://play.google.com/store/apps/details?id=com.edt.ut3&hl=ln) <-- !

## Faites parti du projet !  
Nous avons ouvert un Discord pour les bêtas afin que les testeurs puissent nous contacter plus facilement.  
Il est possible que l'application soit portée sur IOS, nous sommes donc à la recherche de développeurs qui pourraient nous aider à le concevoir en Flutter. N'hésitez donc pas à rejoindre le serveur pour nous en parler :)

## Modifications par rapport à la version 1.x

L'application a été entièrement repensée et entièrement re-codée en Kotlin, l'ancienne version étant en Java.  
Elle profite d'une interface refaite à neuf ainsi que de nouvelles fonctionnalités.  
Parmis elles on pourra noter :
 - L'application est disponible en Français et en Anglais ! Étant donné le nombre d'étudiants étrangers qui pourraient ne pas être à l'aise avec le Français, nous avons décidé de traduire l'application. D'autres langues pourront être proposées mais nous n'effectuerons pas la traduction nous-même.
 On notera cependant que les événements ne sont pas traduits car les données affichées sont celles reçues depuis Celcat.
 - Le fait que l'emploi du temps est compatible avec tous les emplois du temps qui ont une version de Celcat compatible y compris pour les facultés autres que Paul Sabatier. On notera cependant que selon la façon dont les données sont organisées, les affichages sur les événements dans le calendrier seront plus ou moins précis. Par exemple la détection des salles, lieux et noms des cours dans la description des événements ne pourront pas être efféctués dans certains cas.
 - L'enregistrement des notes se fait en temps réel pendant l'édition de ces dernières.
 - Il est maintenant possible de cacher les cours qui ne nous intéressent pas (dans le cas où la détection des noms des matières a fonctionnée)
 - Il est maintenant possible d'ajouter des photos aux notes.
 - L'emploi du temps est maintenant affiché d'une manière différente, deux événements à la même heure seront positionnés côte-à-côte (voir screens plus bas).
 - Deux layouts ont été ajoutées et sont cachées derrière l'emploi du temps. Sur la gauche, une pour afficher les modifications de la dernière sauvegarde. Sur la droite, des options que l'utilisateur pourra régler lui-même comme le fait de régler l'affichage sur certains cours.
 - Un fragment affichant une carte interactive à la manière de Google Maps (pour l'instant avec [OSMDroid](https://github.com/osmdroid/osmdroid)) montre les différents bâtiments de l'université ainsi que ceux du Crous. La recherche de chemin se fait par contre via l'application Google Maps pour des questions de coût, les APIs de ce genre étant pour la plupart payantes.

## Description des fragments  
### Calendrier  

Le calendrier (et l'application en général) dispose d'un thème sombre et d'un thème clair :

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_dark.jpg" height="600" alt="Theme sombre">

Les événements sont maintenant disposés côte à côte lorsque qu'ils sont à la même heure ou bien qu'ils se chevauchent.  
Cela évite donc les "multi-event" utilisés dans l'ancienne version qui demandaient de cliquer dessus puis de traverser les différents événement pour réellement voir quels étaient les cours. Ceci est rendu possible grâce à la librarie [Yoda](https://github.com/axellaffite/yoda), codée pour ce projet.  
À noter qu'une vue __agenda__ est configurée de base et une vue __semaine__ est disponible d'un simple clic grâce à la barre d'action affichée en haut !

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_evenements_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_evenements_dark.jpg" height="600" alt="Theme sombre">
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_evenements_white_week.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/calendrier_evenements_dark_week.jpg" height="600" alt="Theme sombre">


Lors d'un clic sur un événement un fragment souvre par le bas, affichant les détails et la note associée à cet événement (s'il y en a une).  
Ce fragment permet par ailleurs de prendre des photos qui seront associées à la note.

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/event_details_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/event_details_dark.jpg" height="600" alt="Theme sombre">


Les matières peuvent être cachées. Il suffit de cliquer sur l'icône en forme d'oeil que l'on voit dans la barre d'action en haut pour que la page de sélection apparaisse.  
   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/visibilite_cours_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/visibilite_cours_dark.jpg" height="600" alt="Theme sombre">


### Trouver une salle

Cette fonctionnalité n'est disponible que pour l'université Paul Sabatier.  
Elle permet de trouver une salle vide en fonction du bâtiment donné.  
Cela a été rendu possible grâce à [Goulin](https://www.goulin.fr/).  
Le code source est dispo --> [ICI](https://github.com/goulinkh/rooms-finder) <--
La version web se trouve --> [ICI](https://rooms-finder.goulin.fr/) <--  

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/find_a_room_loading_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/find_a_room_loading_dark.jpg" height="600" alt="Theme sombre">
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/find_a_room_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/find_a_room_dark.jpg" height="600" alt="Theme sombre">

L'API retenue a finalement été OSMDroid qui utilise OpenStreeMap.
Pour la recherche de chemin, cela se fera via un Intent Google Maps.

### Carte

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/maps_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/maps_dark.jpg" height="600" alt="Theme sombre">
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/maps_choices_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/maps_choices_dark.jpg" height="600" alt="Theme sombre">

L'API retenue a finalement été OSMDroid qui utilise OpenStreeMap.
Pour la recherche de chemin, cela se fera via un Intent Google Maps.

### Préférences

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/preferences_white.jpg" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/previews/preferences_dark.jpg" height="600" alt="Theme sombre">


Les différentes options qui s'offrent maintenant à l'utilisateur sont :  
 - Le choix du theme (thème du téléphone, clair ou sombre)
 - L'activation ou non des notifications de mises à jour d'emploi du temps. Celles-ci resteront tout de même disponible sur la gauche du calendrier.
 - Le lien vers le calendrier Celcat qui permettra à l'application de récupérer l'emploi du temps. Le choix de la formation ne se fait donc plus via l'onglet de choix de formation pour plusieurs raisons qui sont notamment que cette page n'est pas fixe dans le temps ainsi que certaines formations qui sont présentent dessus ne possède pas d'emploi du temps sur Celcat.

D'autres options sont à l'étude comme la suppression des événements à partir d'un certain temps, la possibilité de n'afficher qu'une seule section (pour ceux qui sont en double licence, enjambement ou autre).
