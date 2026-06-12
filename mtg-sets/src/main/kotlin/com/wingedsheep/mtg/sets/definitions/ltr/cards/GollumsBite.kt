package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gollum's Bite
 * {B}
 * Instant
 *
 * Target creature gets -2/-2 until end of turn.
 * {3}{B}, Exile this card from your graveyard: The Ring tempts you. Activate only as a sorcery.
 *
 * Gap 11 (activated abilities from the graveyard) is already engine-landed (`activateFromZone =
 * Zone.GRAVEYARD`, `Costs.ExileSelf`, `GraveyardAbilityEnumerator`, template Bonebind Orator). The
 * instant's spell composes `Effects.ModifyStats`; the graveyard ability composes the existing
 * exile-from-graveyard cost + Ring tempt, gated to sorcery speed.
 */
val GollumsBite = card("Gollum's Bite") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -2/-2 until end of turn.\n" +
        "{3}{B}, Exile this card from your graveyard: The Ring tempts you. Activate only as a sorcery."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(-2, -2, creature)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Anton Solovianchyk"
        flavorText = "\"It was Pity that stayed Bilbo's hand. Do not be too eager to deal out death in " +
            "judgment; Gollum has some part to play yet, before the end.\"\n—Gandalf, to Frodo"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b1e790e-ff82-4888-8aee-9986c646241a.jpg?1686968460"
    }
}
