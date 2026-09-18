# Commandes OnStep utilisées par le bootstrap

| Fonction | Commande |
|---|---|
| État | `:GU#` |
| RA / DEC | `:GR#` / `:GD#` |
| Déplacement | `:Mn#`, `:Ms#`, `:Me#`, `:Mw#` |
| Stop direction | `:Qn#`, `:Qs#`, `:Qe#`, `:Qw#` |
| Stop global | `:Q#` |
| Vitesses | `:RG#`, `:RC#`, `:RM#`, `:RS#` |
| Suivi | `:Te#` / `:Td#` |
| Park / Unpark | `:hP#` / `:hR#` |
| Retour HOME | `:hC#` |
| Cible GOTO | `:Sr...#`, `:Sd...#`, puis `:MS#` |
| Début alignement | `:A[n]#` |
| Statut alignement | `:A?#` |
| Accepter étoile | `:A+#` |
| Sauvegarder alignement | `:AW#` |

Les réponses ne sont pas uniformes : certaines sont terminées par `#`, certaines valent un octet `0/1`, certaines commandes n'ont aucune réponse. Le client distingue ces trois cas.
