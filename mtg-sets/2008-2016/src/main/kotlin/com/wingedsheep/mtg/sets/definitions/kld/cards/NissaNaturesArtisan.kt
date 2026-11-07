package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Nature's Artisan — Kaladesh #270 (Planeswalker Deck)
 * {4}{G}{G} · Legendary Planeswalker — Nissa · Starting loyalty 5
 *
 * +3: You gain 3 life.
 * −4: Reveal the top two cards of your library. Put all land cards from among them onto the
 *     battlefield and the rest into your hand.
 * −12: Creatures you control get +5/+5 and gain trample until end of turn.
 *
 * The −12 is Overrun's shape: [Effects.ForEachInGroup] fixes the affected set as the ability
 * resolves, so creatures that arrive later in the turn get nothing (ruling).
 */
val NissaNaturesArtisan = card("Nissa, Nature's Artisan") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 5
    oracleText = "+3: You gain 3 life.\n" +
        "−4: Reveal the top two cards of your library. Put all land cards from among them onto the " +
        "battlefield and the rest into your hand.\n" +
        "−12: Creatures you control get +5/+5 and gain trample until end of turn."

    loyaltyAbility(+3) {
        effect = Effects.GainLife(3)
    }

    loyaltyAbility(-4) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(2), revealed = true)
            val (lands, rest) = filterSplit(revealed, GameObjectFilter.Land)
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
            toHand(rest)
        }
    }

    loyaltyAbility(-12) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(5, 5, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "270"
        inBooster = false
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/777533c3-0a87-4625-8987-d850fc236ddb.jpg?1783937134"

        ruling("2016-09-20", "The set of creatures affected by Nissa's last ability is determined as the ability resolves. Creatures you begin to control later in the turn and noncreature permanents that become creatures later in the turn won't get +5/+5 or gain trample.")
    }
}
