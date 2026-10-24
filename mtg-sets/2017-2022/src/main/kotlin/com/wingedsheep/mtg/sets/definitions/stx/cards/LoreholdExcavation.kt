package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lorehold Excavation — Strixhaven: School of Mages #200 (canonical printing)
 * {R}{W} · Enchantment
 *
 * At the beginning of your end step, mill a card. If a land card was milled this way, you gain
 * 1 life. Otherwise, this enchantment deals 1 damage to each opponent.
 * {5}, Exile a creature card from your graveyard: Create a tapped 3/2 red and white Spirit
 * creature token.
 *
 * The end-step trigger is the Loafing Giant shape: a pipeline mill gated by `whenMatches` on the
 * milled collection. An empty library mills nothing, so "otherwise" (no land milled) deals the
 * damage. The activated ability is the Graf Harvest cost over Illustrious Historian's token.
 */
val LoreholdExcavation = card("Lorehold Excavation") {
    manaCost = "{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, mill a card. If a land card was milled this " +
        "way, you gain 1 life. Otherwise, this enchantment deals 1 damage to each opponent. (To mill " +
        "a card, put the top card of your library into your graveyard.)\n" +
        "{5}, Exile a creature card from your graveyard: Create a tapped 3/2 red and white Spirit " +
        "creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val milled = mill(1)
            run(Effects.If(
                condition = whenMatches(milled, GameObjectFilter.Land),
                then = Effects.GainLife(1),
                otherwise = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
            ))
        }
        description = "At the beginning of your end step, mill a card. If a land card was milled " +
            "this way, you gain 1 life. Otherwise, this enchantment deals 1 damage to each opponent."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 2,
            colors = setOf(Color.RED, Color.WHITE),
            creatureTypes = setOf("Spirit"),
            tapped = true
        )
        description = "{5}, Exile a creature card from your graveyard: Create a tapped 3/2 red and " +
            "white Spirit creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Jinho Bae"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43105beb-46f3-4914-8222-4907bd76d48f.jpg?1783927309"
    }
}
