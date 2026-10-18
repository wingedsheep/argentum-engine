package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Norika Yamazaki, the Poet — Kamigawa: Neon Dynasty #31 (canonical printing)
 * {2}{W} · Legendary Creature — Human Samurai · 3/2
 *
 * Vigilance
 * Whenever a Samurai or Warrior you control attacks alone, you may cast target enchantment card
 * from your graveyard this turn.
 *
 * The trigger is the attacks-alone shape of Eiganjo Exemplar; the payoff is Zul Ashur's this-turn
 * may-play permission over the targeted graveyard card where it lies. Normal costs and timing apply,
 * and the card goes to the graveyard (or battlefield) normally afterwards. The "may" is the
 * permission itself — the player chooses whether to cast.
 */
val NorikaYamazakiThePoet = card("Norika Yamazaki, the Poet") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 2
    oracleText = "Vigilance\nWhenever a Samurai or Warrior you control attacks alone, you may cast " +
        "target enchantment card from your graveyard this turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        target(TargetFilter(GameObjectFilter.Enchantment.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.EndOfTurn,
                nonLandOnly = true,
            ))
        }
        description = "Whenever a Samurai or Warrior you control attacks alone, you may cast target " +
            "enchantment card from your graveyard this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Magali Villeneuve"
        flavorText = "\"The blades will have their say, but peace is built with words.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09cb553c-58c1-4cfd-b441-297b5f09b263.jpg?1783923915"
    }
}
