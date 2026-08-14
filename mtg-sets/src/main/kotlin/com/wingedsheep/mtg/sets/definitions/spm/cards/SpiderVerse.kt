package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spider-Verse — Marvel's Spider-Man #93
 * {3}{R}{R} · Enchantment
 *
 * The "legend rule" doesn't apply to Spiders you control.
 * Whenever you cast a spell from anywhere other than your hand, you may copy it. If you do, you
 * may choose new targets for the copy. If the copy is a permanent spell, it gains haste. Do this
 * only once each turn.
 *
 * The legend-rule exemption uses the new `LegendRuleDoesNotApplyTo(filter)` static.
 */
val SpiderVerse = card("Spider-Verse") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "The \"legend rule\" doesn't apply to Spiders you control.\n" +
        "Whenever you cast a spell from anywhere other than your hand, you may copy it. If you do, " +
        "you may choose new targets for the copy. If the copy is a permanent spell, it gains haste. " +
        "Do this only once each turn."

    // The "legend rule" doesn't apply to Spiders you control.
    staticAbility {
        ability = LegendRuleDoesNotApplyTo(GameObjectFilter.Creature.withAnySubtype("Spider"))
    }

    // Whenever you cast a spell from a non-hand zone, you may copy it (once each turn); a permanent
    // copy gains haste.
    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND))
        )
        // CR 603.2h, and the ruling says both halves out loud: "Once you choose to copy a spell
        // with Spider-Verse's last ability, that ability won't trigger again for the duration of
        // the turn. Any instances of the ability already on the stack when you choose to copy a
        // spell will have no effect." Declining leaves the turn's copy unspent.
        effectOncePerTurn = true
        effect = MayEffect(
            effect = Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                addedTokenKeywords = setOf(Keyword.HASTE)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "93"
        artist = "Alexander Gering"
        flavorText = "Every Spider ever...and then some!"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8779eb2-1210-430d-8d42-3077053441ee.jpg?1783905331"
    }
}
