package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hollow Scavenger // Bakery Raid
 * {2}{G}
 * Creature — Wolf
 * 3/2
 * {1}, Sacrifice a Food: This creature gets +2/+2 until end of turn. Activate only once each turn.
 *
 * Adventure: Bakery Raid — {G}, Sorcery — Adventure
 * Create a Food token.
 *
 * "Sacrifice a Food" matches any Food artifact, not just Food tokens (2024-11-08 ruling) — the cost
 * filter is subtype-based (`GameObjectFilter.Any.withSubtype("Food")`), so Tough Cookie and friends
 * are legal fodder too.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile — the Food it left behind then
 * feeds the creature's own pump ability.)
 */
val HollowScavenger = card("Hollow Scavenger") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "{1}, Sacrifice a Food: This creature gets +2/+2 until end of turn. " +
        "Activate only once each turn."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        )
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    adventure("Bakery Raid") {
        manaCost = "{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Create a Food token. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.CreateFood(1)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Michele Giorgi"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ad345b6-7077-4dd2-b515-c774a3185fe4.jpg?1783915081"
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token."
        )
        ruling(
            "2024-11-08",
            "You can't sacrifice a Food to pay multiple costs. For example, you can't sacrifice a Food " +
                "token to activate its own ability and also to activate this ability."
        )
    }
}
