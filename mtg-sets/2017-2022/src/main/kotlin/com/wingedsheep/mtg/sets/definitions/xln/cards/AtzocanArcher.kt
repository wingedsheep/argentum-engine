package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val AtzocanArcher = card("Atzocan Archer") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Archer"
    power = 1
    toughness = 4
    oracleText = "Reach\nWhen this creature enters, you may have it fight another target creature. " +
        "(Each deals damage equal to its power to the other.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val other = target(TargetFilter.OtherCreature)
        optional = true
        effect = Effects.Fight(EffectTarget.Self, other)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Naomi Baker"
        flavorText = "His arrows are fletched with feathers from the greatest of the winged dinosaurs."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e214e0d-bd3d-467b-83fb-d8a232e166e4.jpg?1783935733"
        ruling("2017-09-29", "You must choose another target creature (if able) as Atzocan Archer’s triggered ability is put on the stack. You choose as that ability resolves whether those creatures fight.")
        ruling("2017-09-29", "If Atzocan Archer isn’t on the battlefield as its triggered ability resolves, or if the target of that ability is illegal, no creature will deal or be dealt damage.")
        ruling("2017-09-29", "Atzocan Archer’s ability can target another creature you control (such as a Dinosaur with an enrage ability).")
    }
}
