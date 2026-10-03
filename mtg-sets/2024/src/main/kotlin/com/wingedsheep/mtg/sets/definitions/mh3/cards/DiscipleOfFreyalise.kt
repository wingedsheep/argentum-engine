package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Disciple of Freyalise {3}{G}{G}{G} // Garden of Freyalise
 * Creature — Elf Druid 3/3
 * When this creature enters, you may sacrifice another creature. If you do, you gain X life and
 * draw X cards, where X is that creature's power.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {G}.
 *
 * The sacrifice is a resolution-time choice, not a target: the pipeline offers up to one other
 * creature you control and sacrifices it; "if you do" gates the payoff on the choice being
 * non-empty. X is the sacrificed creature's last-known power (2024-06-07 ruling), read through
 * [DynamicAmounts.sacrificedPower], a live-then-LKI reference.
 */
private val DiscipleOfFreyaliseFront = card("Disciple of Freyalise") {
    manaCost = "{3}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may sacrifice another creature. If you do, you gain X " +
        "life and draw X cards, where X is that creature's power."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val others = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = others,
                useTargetingUI = true,
                prompt = "You may sacrifice another creature",
                selectedLabel = "Sacrifice",
            )
            ifNotEmpty(chosen) {
                sacrifice(chosen)
                run(Effects.GainLife(DynamicAmounts.sacrificedPower()))
                run(Effects.DrawCards(DynamicAmounts.sacrificedPower()))
            }
        }
        description = "When this creature enters, you may sacrifice another creature. If you do, you gain " +
            "X life and draw X cards, where X is that creature's power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Valera Lutfullina"
        flavorText = "\"Freyalise is gone, but her song lives on.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8e9ea5a-5e10-4b77-baef-0352ff035483.jpg?1783911228"
        ruling("2024-06-07", "Use the power of the sacrificed creature as it last existed on the battlefield to determine the value of X.")
    }
}

private val GardenOfFreyaliseBack = card("Garden of Freyalise") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {G}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Valera Lutfullina"
        flavorText = "The Juniper Order safeguards secret places that still thrum with traces of Freyalise's life-giving magic."
        imageUri = "https://cards.scryfall.io/normal/back/a/8/a8e9ea5a-5e10-4b77-baef-0352ff035483.jpg?1783911228"
    }
}

val DiscipleOfFreyalise: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = DiscipleOfFreyaliseFront,
    backFace = GardenOfFreyaliseBack,
)
