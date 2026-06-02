package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Daring Waverider {4}{U}{U}
 * Creature — Otter Wizard
 * 4/4
 *
 * When this creature enters, you may cast target instant or sorcery card with mana value
 * 4 or less from your graveyard without paying its mana cost. If that spell would be put
 * into your graveyard, exile it instead.
 */
val DaringWaverider = card("Daring Waverider") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Otter Wizard"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may cast target instant or sorcery card with mana value 4 or less from your graveyard without paying its mana cost. If that spell would be put into your graveyard, exile it instead."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou().manaValueAtMost(4)
        )
        effect = Effects.Composite(
            listOf(
                // Move the targeted card from graveyard to exile
                Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
                // Grant free cast from exile + exile after resolve
                Effects.GrantFreeCastTargetFromExile(
                    target = EffectTarget.ContextTarget(0),
                    exileAfterResolve = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Wisnu Tan"
        flavorText = "\"Look what I found!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19422406-0c1a-497e-bed1-708bc556491a.jpg?1721426039"
        ruling("2024-07-26", "You cast the spell while Daring Waverider's ability is resolving and still on the stack. You can't wait to cast it later in the turn.")
        ruling("2024-07-26", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2024-07-26", "If an instant or sorcery card you cast this way is countered, it will be exiled.")
    }
}
