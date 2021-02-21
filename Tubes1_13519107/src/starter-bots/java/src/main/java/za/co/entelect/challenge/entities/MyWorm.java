package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class MyWorm extends Worm {
    @SerializedName("weapon")
    public Weapon weapon;

    @SerializedName("snowballs")
    public Snowballs snowballs;

    @SerializedName("bananaBombs")
    public BananaBombs bananaBombs;
}
