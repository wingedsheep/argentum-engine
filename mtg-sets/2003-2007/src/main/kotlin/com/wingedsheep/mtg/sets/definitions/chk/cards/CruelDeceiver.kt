package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruel Deceiver
 * {1}{B}
 * Creature — Spirit
 * 2/1
 * {1}: Look at the top card of your library.
 * {2}: Reveal the top card of your library. If it's a land card, this creature gains "Whenever
 * this creature deals damage to a creature, destroy that creature" until end of turn. Activate
 * only once each turn.
 *
 * Both abilities are a non-destructive pipeline gather of the top card (Feral Deceiver shape) —
 * the card stays on top. The `{1}` ability is a private look; the `{2}` ability reveals it to all
 * players and, on a land, grants the Deceiver an until-end-of-turn triggered ability. The granted
 * trigger fires on *any* damage (not just combat) to a creature, and destroys the damaged creature
 * (the triggering entity). The "once each turn" cap binds only the `{2}` ability.
 */
val CruelDeceiver = card("Cruel Deceiver") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "{1}: Look at the top card of your library.\n" +
        "{2}: Reveal the top card of your library. If it's a land card, this creature gains " +
        "\"Whenever this creature deals damage to a creature, destroy that creature\" until end " +
        "of turn. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.Pipeline {
            gather(CardSource.TopOfLibrary(1))
        }
        description = "{1}: Look at the top card of your library."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.Pipeline {
            val cruelDeceiverRevealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(cruelDeceiverRevealed, GameObjectFilter.Land),
                then = Effects.GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.dealsDamage(Recipient.AnyCreature)
                        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
                    },
                    target = EffectTarget.Self,
                ),
            ))
        }
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        description = "{2}: Reveal the top card of your library. If it's a land card, this creature " +
            "gains \"Whenever this creature deals damage to a creature, destroy that creature\" " +
            "until end of turn. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Nottsuo"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cc6972a-5305-423f-a936-16ee0fbf9200.jpg?1783944316"
    }
}
