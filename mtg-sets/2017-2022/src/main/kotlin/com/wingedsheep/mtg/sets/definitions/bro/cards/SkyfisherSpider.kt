package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Skyfisher Spider
 * {2}{B}{G}
 * Creature — Spider
 * 3/3
 * Reach
 * When this creature enters, you may sacrifice another creature. When you do, destroy target
 * nonland permanent.
 * When this creature dies, you may gain 1 life for each creature card in your graveyard. If you
 * do, exile this card from your graveyard.
 *
 * The enters trigger is Ruthless Lawbringer's shape: the sacrifice is a resolution-time,
 * non-targeting choice, and only once it happens does the reflexive trigger choose its target.
 *
 * The dies trigger's "if you do" is the choice to gain life, so the exile rides inside the same
 * `May`. The exile gathers the card from *your* graveyard by source identity rather than moving
 * `Self`: if the card has already left the graveyard by resolution, the life is still gained but
 * nothing is exiled (CR 400.7 — it is a new object). The count includes the Spider itself.
 */
val SkyfisherSpider = card("Skyfisher Spider") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Spider"
    power = 3
    toughness = 3
    oracleText = "Reach\n" +
        "When this creature enters, you may sacrifice another creature. When you do, destroy target nonland permanent.\n" +
        "When this creature dies, you may gain 1 life for each creature card in your graveyard. If you do, exile this card from your graveyard."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.Pipeline {
                val creatureToSacrifice = selectTarget(
                    TargetObject(
                        filter = TargetFilter.CreatureYouControl.other()
                    ),
                    nonTargeting = true,
                    prompt = "Choose another creature to sacrifice"
                )
                run(Effects.SacrificeTarget(creatureToSacrifice.asTarget))
            },
            optional = true,
            descriptionOverride = "You may sacrifice another creature. When you do, destroy target nonland permanent."
        ) {
            val nonlandPermanent = target(TargetFilter.NonlandPermanent)
            effect = Effects.Destroy(nonlandPermanent)
        }
        description = "When this creature enters, you may sacrifice another creature. When you do, destroy target nonland permanent."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Effects.GainLife(DynamicAmounts.creatureCardsInYourGraveyard()) then
                Effects.Pipeline {
                    val spider = gather(
                        CardSource.FromZone(
                            zone = Zone.GRAVEYARD,
                            player = Player.You,
                            filter = GameObjectFilter.Any.sourceItself()
                        )
                    )
                    move(spider, CardDestination.ToZone(Zone.EXILE))
                },
            descriptionOverride = "You may gain 1 life for each creature card in your graveyard. " +
                "If you do, exile this card from your graveyard."
        )
        description = "When this creature dies, you may gain 1 life for each creature card in your graveyard. " +
            "If you do, exile this card from your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Henry Peters"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87300ac8-8b6f-4c37-8058-beb59eaa66be.jpg?1783920025"

        ruling("2022-10-14", "Skyfisher Spider's first triggered ability doesn't have any targets. If you choose to sacrifice another creature, a second triggered ability (called a \"reflexive\" triggered ability) goes on the stack and you choose a target nonland permanent at that time. Players may respond to that second ability as normal.")
        ruling("2022-10-14", "Notably, this means that players won't get to know what nonland permanent you plan to target before you choose whether to sacrifice a creature.")
    }
}
