package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nullpriest of Oblivion
 * {1}{B}
 * Creature — Vampire Cleric
 * 2/1
 *
 * Kicker {3}{B}
 * Lifelink
 * Menace
 * When this creature enters, if it was kicked, return target creature card from your graveyard to
 *   the battlefield.
 *
 * - Kicker is the [KeywordAbility.kicker] alternative additional cost; the ETB reanimation is gated
 *   by an intervening "if" ([Conditions.WasKicked], CR 603.4) so the trigger only goes on the stack
 *   — and only asks for a graveyard target — when the spell was actually kicked.
 * - The reanimation is a plain graveyard-targeted [Effects.PutOntoBattlefield].
 *
 * Canonical printing lives here (Zendikar Rising, the earliest real printing); Foundations reprints
 * it (see the FDN `Printing` row).
 */
val NullpriestOfOblivion = card("Nullpriest of Oblivion") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    power = 2
    toughness = 1
    oracleText = "Kicker {3}{B} (You may pay an additional {3}{B} as you cast this spell.)\n" +
        "Lifelink\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature enters, if it was kicked, return target creature card from your " +
        "graveyard to the battlefield."

    keywords(Keyword.LIFELINK, Keyword.MENACE)
    keywordAbility(KeywordAbility.kicker("{3}{B}"))

    // When this creature enters, if it was kicked, return target creature card from your graveyard
    // to the battlefield.
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        val t = target(TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOntoBattlefieldFromGraveyard(t)
        description = "When this creature enters, if it was kicked, return target creature card " +
            "from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/086fc7fb-efcf-4676-8455-39b63edaec6a.jpg?1783929369"
    }
}
