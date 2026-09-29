package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nissa, Ascended Animist — Phyrexia: All Will Be One #175
 * {3}{G}{G}{G/P}{G/P} · Legendary Planeswalker — Nissa · Starting loyalty 7
 *
 * Compleated
 * +1: Create an X/X green Phyrexian Horror creature token, where X is Nissa's loyalty.
 * −1: Destroy target artifact or enchantment.
 * −7: Until end of turn, creatures you control get +1/+1 for each Forest you control and gain trample.
 *
 * Compleated is read by the engine at resolution: each {G/P} paid with 2 life takes two
 * loyalty counters off the seven she enters with. The −7's bonus is counted once, as it resolves.
 */
val NissaAscendedAnimist = card("Nissa, Ascended Animist") {
    manaCost = "{3}{G}{G}{G/P}{G/P}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 7
    oracleText = "Compleated ({G/P} can be paid with {G} or 2 life. For each {G/P} paid with life, this planeswalker enters with two fewer loyalty counters.)\n" +
        "+1: Create an X/X green Phyrexian Horror creature token, where X is Nissa's loyalty.\n" +
        "−1: Destroy target artifact or enchantment.\n" +
        "−7: Until end of turn, creatures you control get +1/+1 for each Forest you control and gain trample."

    keywords(Keyword.COMPLEATED)

    loyaltyAbility(+1) {
        val loyalty = DynamicAmounts.countersOnSelf(CounterType.LOYALTY)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Phyrexian", "Horror"),
            dynamicPower = loyalty,
            dynamicToughness = loyalty,
            imageUri = "https://cards.scryfall.io/normal/front/1/f/1f5fc2cb-a172-418a-a0f3-1a63a5f1aa2c.jpg?1783918168"
        )
    }

    loyaltyAbility(-1) {
        val permanent = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(permanent)
    }

    loyaltyAbility(-7) {
        val forests = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype(Subtype.FOREST)).count()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(forests, forests, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "175"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dd64b1d-bcef-476c-bf0b-3ac7df7cbed3.jpg?1783918012"
        ruling("2023-02-04", "The token created by Nissa's first ability has its power and toughness set only once, at the time the ability resolves. It doesn't change later as loyalty counters are added to or removed from Nissa.")
        ruling("2023-02-04", "If Nissa is no longer on the battlefield at the time her first ability resolves, use the number of loyalty counters she had when she last existed on the battlefield to determine the value of X.")
        ruling("2023-02-04", "The power and toughness bonus granted to creatures by Nissa's last ability is determined only once, as that ability resolves. It won't increase or decrease if the number of Forests you control later changes. Similarly, it applies only to creatures you control at the time it resolves. Creatures that come under your control after it resolves will not have the bonus.")
        ruling("2023-02-04", "A Phyrexian mana symbol contributes 1 toward the mana value of a card, even if life is paid for it. Specifically, Nissa's mana value is always 7.")
        ruling("2023-02-04", "The compleated ability looks only at whether a player chose to pay 2 life for a Phyrexian mana symbol as they were casting the spell. If a player paid life for some other reason while casting the spell, that will not reduce the number of loyalty counters the planeswalker enters the battlefield with.")
        ruling("2023-02-04", "Other replacement effects that would change the number of loyalty counters Nissa enters with will apply as normal.")
    }
}
