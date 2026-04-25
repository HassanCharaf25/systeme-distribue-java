import java.io.Serializable;

/**
 * Représente une ressource (Nom et Quantité)
 * Utilisé pour stocker les éléments d'une réaction
 * Implemente Serializable pour la transformation de l'objet en flux d'octets pour garantir la transmission via réseau
 * 
 * @author CHARAF Hassan
 */

public class Ressource implements Serializable {
    private String nom;
    private int quantite;

    public Ressource(String nom, int quantite) {
        this.nom = nom;
        this.quantite = quantite;
    }

    public String getNom() {
        return nom;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    @Override
    public String toString() {
        return quantite + " " + nom;
    }
}