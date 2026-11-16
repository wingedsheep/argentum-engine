package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arashin Sovereign
 * {5}{G}{W}
 * Creature — Dragon
 * 6/6
 * Flying
 * When this creature dies, you may put it on your choice of the top or bottom of its owner's library.
 *
 * "Your choice" is the trigger's controller (who controlled it when it died), not the card's owner,
 * so the position is a controller-routed `ChooseAction` between the top and bottom moves rather than
 * `PutOnTopOrBottomOfLibrary` (which asks the owner). The "may" is the optional trigger.
 */
val ArashinSovereign = card("Arashin Sovereign") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "When this creature dies, you may put it on your choice of the top or bottom of its owner's library."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        optional = true
        effect = Effects.ChooseAction(
            choices = listOf(
                EffectChoice("Put it on top of its owner's library", Effects.PutOnTopOfLibrary(EffectTarget.Self)),
                EffectChoice("Put it on the bottom of its owner's library", Effects.PutOnBottomOfLibrary(EffectTarget.Self)),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Dan Murayama Scott"
        flavorText = "Dromoka dragons foster trust among their subjects, while the other clans must spend their time quelling rebellion."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db7fbf78-8cb6-4fbf-b925-5543bcb64b45.jpg?1783938575"
        ruling(
            "2015-02-25",
            "The player who controlled Arashin Sovereign when it died chooses whether to put it on the top or " +
                "bottom of its owner's library. The player may also choose to leave Arashin Sovereign in the graveyard. " +
                "This choice is made as the ability resolves."
        )
        ruling(
            "2015-02-25",
            "Arashin Sovereign will be put on the top or bottom of that library only if it's still in the graveyard " +
                "when its ability resolves. If it leaves the graveyard before that point, it will stay in whatever zone " +
                "it's in, even if it's returned to the graveyard before the ability resolves."
        )
    }
}
