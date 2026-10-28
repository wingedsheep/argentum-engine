package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Squad Commander — Zendikar Rising #41
 * {3}{W} · Creature — Kor Warrior · 3/3
 *
 * When this creature enters, create a 1/1 white Kor Warrior creature token for each creature in
 * your party.
 * At the beginning of combat on your turn, if you have a full party, creatures you control get
 * +1/+0 and gain indestructible until end of turn.
 */
val SquadCommander = card("Squad Commander") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Warrior"
    oracleText = "When this creature enters, create a 1/1 white Kor Warrior creature token for each creature " +
        "in your party. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "At the beginning of combat on your turn, if you have a full party, creatures you control get +1/+0 " +
        "and gain indestructible until end of turn."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.partySize(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Kor", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/1/3/13412797-f831-49fe-adf3-369cdfacdbd5.jpg?1783929500",
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.FullParty()
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = Effects.ModifyStats(1, 0, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity, Duration.EndOfTurn),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2ce186d-c9cd-41e9-bac2-ad9d36c4939f.jpg"
    }
}
