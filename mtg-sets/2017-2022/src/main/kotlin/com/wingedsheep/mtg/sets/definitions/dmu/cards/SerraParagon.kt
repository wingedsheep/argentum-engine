package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serra Paragon — Dominaria United #32
 *
 * One graveyard grant covers both halves: `playsLands` lets it play land cards, and a land's mana
 * value is 0, so `Permanent.manaValueAtMost(3)` admits every land as well as the cheap permanent
 * spells. The once-per-turn allowance sits on Serra Paragon, so a land and a spell share it. The
 * granted trigger rides the grant onto whatever entered through it, and stays after Paragon leaves.
 */
val SerraParagon = card("Serra Paragon") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Once during each of your turns, you may play a land from your graveyard or cast a permanent " +
        "spell with mana value 3 or less from your graveyard. If you do, it gains \"When this permanent " +
        "is put into a graveyard from the battlefield, exile it and you gain 2 life.\""

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Permanent.manaValueAtMost(3),
            duringYourTurnOnly = true,
            oncePerTurn = true,
            playsLands = true,
            grantedAbilityOnEntry = TriggeredAbility.create(
                trigger = Triggers.self.dies(),
                effect = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD) then
                    Effects.GainLife(2),
                descriptionOverride = "When this permanent is put into a graveyard from the battlefield, " +
                    "exile it and you gain 2 life.",
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "32"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce295f1e-fb31-4275-a5d3-8c6f29afff40.jpg?1783921359"
    }
}
