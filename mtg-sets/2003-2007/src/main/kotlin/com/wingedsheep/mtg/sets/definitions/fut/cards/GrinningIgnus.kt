package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Grinning Ignus — Future Sight #116 (canonical printing)
 * {2}{R} · Creature — Elemental · 2/2
 *
 * {R}, Return this creature to its owner's hand: Add {C}{C}{R}. Activate only as a sorcery.
 *
 * A mana ability with sorcery timing (CR 602.5d): it doesn't use the stack, but it is offered only
 * when its controller could cast a sorcery, so auto-pay never uses it mid-cast — the player
 * activates it first and spends the floating mana.
 */
val GrinningIgnus = card("Grinning Ignus") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "{R}, Return this creature to its owner's hand: Add {C}{C}{R}. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.ReturnSelfToHand)
        effect = Effects.AddColorlessMana(2) then Effects.AddMana(Color.RED, 1)
        manaAbility = true
        timing = TimingRule.SorcerySpeed
        description = "{R}, Return this creature to its owner's hand: Add {C}{C}{R}. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "James Kei"
        flavorText = "\"Take care what you offer the ignus. Food, perhaps. Coins. But nothing flammable!\"\n—Stovic, village eccentric"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31716a43-2522-46ff-a9a4-b6952cd41f11.jpg?1783943102"
        ruling("2021-03-19", "Although Grinning Ignus's ability has a timing restriction, it's still a mana ability. It doesn't use the stack and it can't be responded to.")
        ruling("2021-03-19", "If you cast Grinning Ignus during your main phase, it will enter the battlefield and you'll receive priority. If no abilities trigger because of this, you can activate its ability immediately, before any other player has a chance to remove it from the battlefield.")
    }
}
