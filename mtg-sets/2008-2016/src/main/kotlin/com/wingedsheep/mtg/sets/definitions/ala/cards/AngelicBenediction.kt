package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Angelic Benediction — Shards of Alara #3 (canonical printing)
 * {3}{W} · Enchantment
 *
 * Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)
 * Whenever a creature you control attacks alone, you may tap target creature.
 *
 * Exalted is the printed keyword; the engine derives its trigger from the projected keyword on any
 * permanent, not just creatures. The second line is the "attacks alone" shape of Asari Captain over
 * any creature you control ([AttackPredicate.Alone]), with the optional tap of Seasoned Marshal.
 */
val AngelicBenediction = card("Angelic Benediction") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 " +
        "until end of turn.)\nWhenever a creature you control attacks alone, you may tap target creature."

    keywords(Keyword.EXALTED)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks(setOf(AttackPredicate.Alone))
        optional = true
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t)
        description = "Whenever a creature you control attacks alone, you may tap target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Michael Komarck"
        flavorText = "\"Even in single combat, I am never alone.\"\n—Rafiq of the Many"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd1b9071-7dde-4128-8b18-1d7b07904638.jpg?1783942585"
    }
}
