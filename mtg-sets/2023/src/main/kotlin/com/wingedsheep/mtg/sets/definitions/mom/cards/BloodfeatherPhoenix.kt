package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodfeather Phoenix (MOM #132)
 * {1}{R} Creature — Phoenix, 2/2
 *
 * A graveyard-active damage observer: the trigger functions only while the card is in its owner's
 * graveyard, watches every instant or sorcery spell its owner controls, and fires once per damaged
 * opponent or battle.
 */
val BloodfeatherPhoenix = card("Bloodfeather Phoenix") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    oracleText = "Flying\n" +
        "This creature can't block.\n" +
        "Whenever an instant or sorcery spell you control deals damage to an opponent or battle, " +
        "you may pay {R}. If you do, return this card from your graveyard to the battlefield. " +
        "It gains haste until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantBlock(GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.InstantOrSorcery.youControl()).dealsDamage(Recipient.OpponentOrBattle)
        triggerZone = Zone.GRAVEYARD
        effect = Effects.MayPay(
            cost = ManaCost.parse("{R}"),
            then = Effects.Move(EffectTarget.Self, Zone.BATTLEFIELD) then
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "132"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed97bf6f-726c-40aa-bc1c-14fa801da96a.jpg?1783916997"
    }
}
