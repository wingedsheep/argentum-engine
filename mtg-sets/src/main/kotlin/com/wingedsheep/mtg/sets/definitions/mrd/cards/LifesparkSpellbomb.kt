package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect

/**
 * Lifespark Spellbomb — Mirrodin #197
 * {1} · Artifact
 *
 * {G}, Sacrifice this artifact: Until end of turn, target land becomes a 3/3 creature that's
 *   still a land.
 * {1}, Sacrifice this artifact: Draw a card.
 *
 * Modelling notes:
 * - [Effects.AnimateLand] is exactly the "becomes an X/Y creature; it's still a land" shape —
 *   it adds the Creature type in layer 4 and *sets* base P/T in layer 7b without touching the
 *   land's types or abilities, so the target keeps its mana ability and any subtypes.
 * - Both abilities sacrifice the Spellbomb as a cost, so the artifact is already in the
 *   graveyard when the ability resolves; the animate effect is independent of its source and
 *   still resolves normally.
 * - The animate ability targets **any** land, not just one you control — matching the printed
 *   text — which is occasionally relevant as a way to make an opponent's land die to a sweeper.
 */
val LifesparkSpellbomb = card("Lifespark Spellbomb") {
    manaCost = "{1}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{G}, Sacrifice this artifact: Until end of turn, target land becomes a 3/3 creature " +
        "that's still a land.\n" +
        "{1}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.SacrificeSelf)
        val land = target("target land", Targets.Land)
        effect = Effects.AnimateLand(land, power = 3, toughness = 3)
        description = "{G}, Sacrifice this artifact: Until end of turn, target land becomes a 3/3 creature that's still a land."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeSelf)
        effect = DrawCardsEffect(1)
        description = "{1}, Sacrifice this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "197"
        artist = "Jim Nelson"
        flavorText = "\"Awaken that which was never asleep.\"\n—Spellbomb inscription"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0adde668-67af-4a08-a36a-2a49893ab20d.jpg?1783944515"
    }
}
