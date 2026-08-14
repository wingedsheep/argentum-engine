package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serpent Specialist — Marvel Super Heroes #186 (common)
 * {G} · Creature — Human Snake Villain · 1/1
 *
 * Deathtouch
 * Power-up — {3}{G}: Put two +1/+1 counters on this creature. (Activate each power-up ability
 * only once. Reduce the cost by its mana cost if it entered this turn.)
 *
 * The cheapest power-up in the set: `{3}{G}` − `{G}` = `{3}`, so a one-drop deathtoucher can
 * become a 3/3 deathtoucher for four mana total on the turn it lands.
 */
val SerpentSpecialist = card("Serpent Specialist") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Snake Villain"
    oracleText = "Deathtouch\n" +
        "Power-up — {3}{G}: Put two +1/+1 counters on this creature. (Activate each power-up " +
        "ability only once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 1
    toughness = 1

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{3}{G}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Daniel Landerman"
        flavorText = "\"A wonderful score, my pet. I can't wait to see what we slither away with tomorrow.\"\n—Princess Python, Zelda DuBois"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72727c2a-08a9-47bc-a5eb-cda52e21684b.jpg?1783902912"
    }
}
