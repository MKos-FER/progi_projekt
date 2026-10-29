Ovdje staviti `.sql` datoteke u formatu [objašnjenom ovdije](https://kgs-software.atlassian.net/wiki/spaces/DOCUEN/pages/2774302818/Flyway)

Prefix može biti:

V - npr V###__<description>.sql
R - R__<description>.sql

V su migracije koje se izvode jednom i onda nikada više i to redosljedom koji je dan njihovom verzijom.
Datoteke sa prefiksom V nemojte mijenjati nakon što se migracija izvede jer se neće izvesti opet bez
da se baza ne izbriše.

R su ponovljive migracije i izvode se nakon svih V migracija. Kada se datoteka s prefiksom R modificira
ta migracija će biti ponovljena pri idućem paljenju baze.
