# Calendrier UT3 (Anciennement EDT UT3) ![Logo](https://github.com/ElZozor/ut3_calendar/blob/master/logo/ic_launcher/res/mipmap-mdpi/ic_launcher.png?raw=true)

Cette application permet de consulter le calendrier Celcat de l'université Paul Sabatier directement sur votre téléphone.  
Disponible pour Android --> [ici](https://play.google.com/store/apps/details?id=com.edt.ut3&hl=ln) <-- !

## Faites partit du projet !  
Nous avons ouvert un Discord pour les bêtas afin que les testeurs puissent nous contacter plus facilement.  
Il est possible que l'application soit portée sur IOS, nous sommes donc à la recherche de développeurs qui pourraient nous aider à le concevoir en Flutter. N'hésitez donc pas à rejoindre le serveur pour nous en parler :)

## Modifications par rapport à la version 1.x

L'application a été entièrement repensée et entièrement re-codée en Kotlin, l'ancienne version étant en Java.  
Elle profite d'une interface refaite à neuf ainsi que de nouvelles fonctionnalités.  
Parmis elles on pourra noter :
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
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/calendrier_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/calendrier_dark.png" height="600" alt="Theme sombre">

Les événements sont maintenant disposés côte à côte lorsque qu'ils sont à la même heure ou bien qu'ils se chevauchent.  
Cela évite donc les "multi-event" utilisés dans l'ancienne version qui demandaient de cliquer dessus puis de traverser les différents événement pour réellement voir quels étaient les cours. Ceci est rendu possible grâce à la librarie [Yoda](https://github.com/ElZozor/yoda), codée pour ce projet.

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/calendrier_evenements_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/calendrier_evenements_dark.png" height="600" alt="Theme sombre">


Lors d'un clic sur un événement un fragment souvre par le bas, affichant les détails et la note associée à cet événement (s'il y en a une).  
Ce fragment permet par ailleurs de prendre des photos qui seront associées à la note.

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/event_details_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/event_details_dark.png" height="600" alt="Theme sombre">

### Préférences

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/preferences_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/preferences_dark.png" height="600" alt="Theme sombre">


Les différentes options qui s'offrent maintenant à l'utilisateur sont :  
 - Le choix du theme (thème du téléphone, clair, sombre ou définir une plage horraire)
   - Pour ce qui est de la plage horraire, on définit tout simplement une plage horraire pendant laquelle le thème sombre s'activera.
 - L'activation ou non des notifications de mises à jour d'emploi du temps. Celles-ci resteront tout de même disponible sur la gauche du calendrier.
 - Le lien vers le calendrier Celcat qui permettra à l'application de récupérer l'emploi du temps. Le choix de la formation ne se fait donc plus via l'onglet de choix de formation pour plusieurs raisons qui sont notamment que cette page n'est pas fixe dans le temps ainsi que certaines formations qui sont présentent dessus ne possède pas d'emploi du temps sur Celcat.

D'autres options sont à l'étude comme la suppression des événements à partir d'un certain temps, la possibilité de n'afficher qu'une seule section (pour ceux qui sont en double licence, enjambement ou autre).

### Carte

   Theme Clair   |   Theme sombre
:---------------:|:-----------------:
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/maps_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/maps_dark.png" height="600" alt="Theme sombre">
<img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/maps_choices_white.png" height="600" alt="Theme clair"> | <img src="https://raw.githubusercontent.com/ElZozor/ut3_calendar/master/previews/maps_choices_dark.png" height="600" alt="Theme sombre">

L'API retenue a finalement été OSMDroid qui utilise OpenStreeMap.
Pour la recherche de chemin, cela se fera via un Intent Google Maps.