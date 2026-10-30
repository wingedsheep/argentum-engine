package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sylvan Offering — Commander 2014 #48 (canonical / earliest printing)
 * {X}{G} · Sorcery
 *
 * Choose an opponent. You and that player each create an X/X green Treefolk creature token.
 * Choose an opponent. You and that player each create X 1/1 green Elf Warrior creature tokens.
 *
 * Intellectual Offering's shape: neither opponent is a target (ruling), so each "choose an
 * opponent" is a resolution-time [Effects.ChooseOpponent] read back through
 * [Player.ChosenOpponent]; the second choice overwrites the first, so the two halves may name
 * different opponents. The chosen opponent's tokens are created under their control via the
 * token `controller`; X is the spell's X throughout.
 */
val SylvanOffering = card("Sylvan Offering") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose an opponent. You and that player each create an X/X green Treefolk creature token.\n" +
        "Choose an opponent. You and that player each create X 1/1 green Elf Warrior creature tokens."

    spell {
        fun treefolk(controller: EffectTarget?) = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Treefolk"),
            controller = controller,
            dynamicPower = DynamicAmounts.xValue(),
            dynamicToughness = DynamicAmounts.xValue(),
            imageUri = "https://cards.scryfall.io/normal/front/2/a/2a3f0d52-34cd-4095-bfa0-bbf9562a8146.jpg?1783938794",
        )
        fun elfWarriors(controller: EffectTarget?) = Effects.CreateToken(
            count = DynamicAmounts.xValue(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior"),
            controller = controller,
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a1ad366d-645c-4b14-8e51-437431eb0fa5.jpg?1783938794",
        )
        val chosenOpponent = EffectTarget.PlayerRef(Player.ChosenOpponent)

        effect = Effects.ChooseOpponent("Choose an opponent: you and that player each create an X/X Treefolk") then
            treefolk(null) then
            treefolk(chosenOpponent) then
            Effects.ChooseOpponent("Choose an opponent: you and that player each create X 1/1 Elf Warriors") then
            elfWarriors(null) then
            elfWarriors(chosenOpponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/584ffe37-cc49-410d-b8a6-06d20d43b36d.jpg?1783938864"
        ruling(
            "2014-11-07",
            "You may choose the same opponent for each of the effects, or you may choose different " +
                "opponents. None of the affected players are targets of the spell."
        )
        ruling("2014-11-07", "You choose the opponents for each effect as the spell resolves.")
    }
}
