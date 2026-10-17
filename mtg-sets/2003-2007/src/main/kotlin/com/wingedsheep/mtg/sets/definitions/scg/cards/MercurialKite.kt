package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Mercurial Kite
 * {3}{U}
 * Creature — Bird
 * 2/1
 * Flying
 * Whenever Mercurial Kite deals combat damage to a creature,
 * tap that creature. It doesn't untap during its controller's next untap step.
 */
val MercurialKite = card("Mercurial Kite") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhenever Mercurial Kite deals combat damage to a creature, tap that creature. It doesn't untap during its controller's next untap step."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.Tap(EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(AbilityFlag.DOESNT_UNTAP, EffectTarget.TriggeringEntity, Duration.UntilAfterAffectedControllersNextUntap())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Richard Sardinha"
        flavorText = "Aven scouts admired the kites' ability to identify and|track their prey. Commanders admired their ability to|knock that prey out of the sky."
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6bc8655-ae27-40be-8d61-e80a5924e955.jpg?1562533105"
    }
}
