package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aragorn, King of Gondor
 * {1}{U}{R}{W}
 * Legendary Creature — Human Noble
 * 4/4
 * Vigilance, lifelink
 * When Aragorn enters, you become the monarch.
 * Whenever Aragorn attacks, up to one target creature can't block this turn. If you're the
 * monarch, creatures can't block this turn.
 *
 * The monarch check is a resolution-time "if" (not an intervening-if), so the trigger always goes
 * on the stack. Choosing no target is allowed; a chosen target that has become illegal fizzles
 * the whole ability, monarch rider included (2023-06-16 ruling) — the engine's standard
 * all-targets-illegal rule.
 */
val AragornKingOfGondor = card("Aragorn, King of Gondor") {
    manaCost = "{1}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Human Noble"
    power = 4
    toughness = 4
    oracleText = "Vigilance, lifelink\n" +
        "When Aragorn enters, you become the monarch.\n" +
        "Whenever Aragorn attacks, up to one target creature can't block this turn. If you're " +
        "the monarch, creatures can't block this turn."

    keywords(Keyword.VIGILANCE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.BecomeMonarch()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.CantBlock(creature) then
            Effects.If(
                condition = Conditions.YouAreTheMonarch,
                then = Effects.CantBlockGroup(GroupFilter.AllCreatures),
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "Yongjae Choi"
        flavorText = "\"In this place will I abide, and my heirs, unto the ending of the world.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c7ac04c-8510-468b-aa6f-f249bde9ff87.jpg?1783916037"
        ruling("2023-06-16", "When Aragorn, King of Gondor's last ability triggers, you can choose not to target a creature, probably because you're the monarch and just want creatures to be unable to block this turn. However, if you do choose a target, and that target is illegal at the time the ability tries to resolve, the ability won't resolve and none of its effects will happen. Creatures will still be able to block this turn, even if you're the monarch.")
        ruling("2023-06-16", "The game starts with no monarch. Once an effect makes one player the monarch, the game will have exactly one monarch from that point forward. As a player becomes the monarch, the current monarch (if any) ceases being the monarch.")
        ruling("2023-06-16", "There are two inherent triggered abilities associated with being the monarch. These triggered abilities have no source and are controlled by the player who was the monarch at the time the abilities triggered. The full texts of these abilities are \"At the beginning of the monarch's end step, that player draws a card\" and \"Whenever a creature deals combat damage to the monarch, its controller becomes the monarch.\"")
    }
}
