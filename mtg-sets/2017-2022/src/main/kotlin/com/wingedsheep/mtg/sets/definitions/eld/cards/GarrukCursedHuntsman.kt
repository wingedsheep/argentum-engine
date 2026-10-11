package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Garruk, Cursed Huntsman — Throne of Eldraine #191 (canonical printing; reprinted in BLC)
 * {4}{B}{G} · Legendary Planeswalker — Garruk · Loyalty 5
 *
 * 0: Create two 2/2 black and green Wolf creature tokens with "When this token dies, put a
 *    loyalty counter on each Garruk you control."
 * −3: Destroy target creature. Draw a card.
 * −6: You get an emblem with "Creatures you control get +3/+3 and have trample."
 *
 * The Wolves' dies trigger is an untargeted `ForEachInGroup` over every permanent with the Garruk
 * subtype its controller controls — any Garruk, not just the one that made it. The −3's draw is
 * unconditional once the ability resolves, so an indestructible target still draws a card.
 */
val GarrukCursedHuntsman = card("Garruk, Cursed Huntsman") {
    manaCost = "{4}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Garruk"
    startingLoyalty = 5
    oracleText = "0: Create two 2/2 black and green Wolf creature tokens with \"When this token dies, " +
        "put a loyalty counter on each Garruk you control.\"\n" +
        "−3: Destroy target creature. Draw a card.\n" +
        "−6: You get an emblem with \"Creatures you control get +3/+3 and have trample.\""

    // 0: Create two 2/2 black and green Wolf tokens with a dies trigger.
    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Wolf"),
            count = 2,
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.ForEachInGroup(
                        GroupFilter(GameObjectFilter.Permanent.withSubtype("Garruk").youControl()),
                        Effects.AddCounters(CounterType.LOYALTY, 1, EffectTarget.IterationEntity),
                    ),
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/88452ed7-1065-41c3-94a6-dc41108c45c1.jpg?1783932479",
        )
    }

    // −3: Destroy target creature. Draw a card.
    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.DrawCards(1)
    }

    // −6: Emblem with "Creatures you control get +3/+3 and have trample."
    loyaltyAbility(-6) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter.AllCreaturesYouControl,
            powerBonus = 3,
            toughnessBonus = 3,
            grantedKeywords = listOf(Keyword.TRAMPLE.name),
            emblemDescription = "Creatures you control get +3/+3 and have trample.",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abef512f-8f1d-4257-b16f-c0eed58670ec.jpg?1783932597"
        ruling("2019-10-04", "If lethal damage is dealt to one of Garruk's Wolf tokens at the same time that Garruk's loyalty is brought to 0 or less, Garruk is put into your graveyard before the Wolf's triggered ability can save him.")
        ruling("2019-10-04", "If the target creature is an illegal target by the time Garruk's second ability tries to resolve, the ability won't resolve. You won't draw a card. If the target is legal but not destroyed (most likely because it has indestructible), you will still draw.")
    }
}
