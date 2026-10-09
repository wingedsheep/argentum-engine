package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temur Sabertooth
 * {2}{G}{G}
 * Creature — Cat
 * 4/3
 *
 * {1}{G}: You may return another creature you control to its owner's hand. If you do, this
 * creature gains indestructible until end of turn.
 *
 * The bounce doesn't target (2014-11-24 ruling: the choice is made on resolution), so it is a
 * gather → `chooseExactly(1)` → `toHand` pipeline inside [Effects.IfYouDo]; indestructible is
 * granted only when a creature was actually chosen and returned.
 */
val TemurSabertooth = card("Temur Sabertooth") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 4
    toughness = 3
    oracleText = "{1}{G}: You may return another creature you control to its owner's hand. If you do, " +
        "this creature gains indestructible until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val candidates = gather(
                        CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Creature,
                            player = Player.You,
                            excludeSelf = true
                        )
                    )
                    // Named: a stolen creature goes to its owner's hand, so "if you do" reads the
                    // choice rather than the growth of your own hand.
                    val bounced = chooseExactly(
                        1,
                        from = candidates,
                        prompt = "Return another creature you control to its owner's hand",
                        useTargetingUI = true,
                        name = "bounced"
                    )
                    toHand(bounced)
                },
                then = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
                successCriterion = SuccessCriterion.CollectionNonEmpty("bounced")
            )
        )
        description = "{1}{G}: You may return another creature you control to its owner's hand. " +
            "If you do, this creature gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Mike Sass"
        flavorText = "The Temur see themselves as a pack, their bonds more primal than the Abzan's."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d54da7c-8828-4d34-bfd0-a654692d3f5a.jpg?1783938677"
        ruling(
            "2014-11-24",
            "You choose whether to return a creature and which creature to return as the activated " +
                "ability resolves. This doesn't target any creature."
        )
    }
}
