import java.util.ArrayList;
import java.util.List;

/**
 * Représente une règle de réaction
 * Sert à séparer en deux listes de ressources, une pour les consommables et une pour les produits ce qui facilitera à l'executeur a savoir quoi verrouiller et quoi produire
 * 
 * @author CHARAF Hassan
 */
public class Reaction {
    private List<Ressource> consommables;
    private List<Ressource> produits;
    private String originalString;

    public Reaction(String reactionStr) {
        this.originalString = reactionStr;
        this.consommables = new ArrayList<>();
        this.produits = new ArrayList<>();
        parse(reactionStr);
    }
    // Sépare les réactifs(partie gauche par rapport à "->") et les consommables(partie droite par rapport à "->") en tableau de chaines de caracteres
    private void parse(String reactionStr) {
        String[] sides = reactionStr.split("->");
        if (sides.length != 2) throw new IllegalArgumentException("Format invalide: " + reactionStr);

        parseComponent(sides[0], consommables);
        parseComponent(sides[1], produits);
    }

    // Sépare les differentes ressources par rapport au "+" et remplit la liste donnée en argument
    private void parseComponent(String side, List<Ressource> targetList) {
        String[] parts = side.split("\\+");
        for (String p : parts) {
            p = p.trim();
            int i = 0;
            // boucle jusqu'a arriver a la fin ou jusqu'a trouver un caractère
            while (i < p.length() && Character.isDigit(p.charAt(i))) i++;
            
            int quantite = (i == 0) ? 1 : Integer.parseInt(p.substring(0, i));
            String name = p.substring(i);
            targetList.add(new Ressource(name, quantite));
        }
    }

    public List<Ressource> getConsommables() { return consommables; }
    public List<Ressource> getProduits() { return produits; }
    
    @Override
    public String toString() { return originalString; }
}