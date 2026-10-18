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
 * Heiko Yamazaki, the General — Kamigawa: Neon Dynasty #146 (canonical printing)
 * {3}{R} · Legendary Creature — Human Samurai · 3/3
 *
 * Trample
 * Whenever a Samurai or Warrior you control attacks alone, you may cast target artifact card
 * from your graveyard this turn.
 *
 * The artifact twin of Norika Yamazaki, the Poet: the attacks-alone trigger grants a this-turn
 * may-play permission over the targeted graveyard card where it lies. Normal costs and timing apply.
 */
val HeikoYamazakiTheGeneral = card("Heiko Yamazaki, the General") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 3
    oracleText = "Trample\nWhenever a Samurai or Warrior you control attacks alone, you may cast " +
        "target artifact card from your graveyard this turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        target(TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.EndOfTurn,
                nonLandOnly = true,
            ))
        }
        description = "Whenever a Samurai or Warrior you control attacks alone, you may cast target " +
            "artifact card from your graveyard this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "146"
        artist = "Magali Villeneuve"
        flavorText = "\"I prefer to let my sword do the talking.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ea5e61c-d903-410b-9acf-96a917ce05cc.jpg?1783923865"
    }
}
