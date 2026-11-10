package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harness the Storm
 * {2}{R}
 * Enchantment
 *
 * Whenever you cast an instant or sorcery spell from your hand, you may cast target card with the
 * same name as that spell from your graveyard. (You still pay its costs.)
 *
 * - Trigger: a cast trigger narrowed to instants and sorceries cast from the hand, so the spell
 *   this cast puts on the stack (cast from the graveyard) doesn't trigger it again.
 * - Target: chosen as the trigger goes on the stack — any card you own in your graveyard whose
 *   name equals the triggering spell's. The name is a value read off the triggering object, so a
 *   spell countered in response still names the card and the cast still happens (2016-04-08
 *   ruling).
 * - Cast: during resolution, paying its costs ([Effects.CastFromCollection], the "you may cast it"
 *   spelling — no free cast, no exile rider), so the spell goes to the graveyard as it resolves.
 *   Declining, or being unable to cast it, leaves the card where it is.
 */
val HarnessTheStorm = card("Harness the Storm") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell from your hand, you may cast target " +
        "card with the same name as that spell from your graveyard. (You still pay its costs.)"

    triggeredAbility {
        trigger = Triggers.you.casts(
            GameObjectFilter.InstantOrSorcery,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        target(
            TargetFilter(
                GameObjectFilter.Any.ownedByYou().sharingNameWith(EffectTarget.TriggeringEntity),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.Pipeline {
            val card = gather(CardSource.ChosenTargets)
            run(
                Effects.May(
                    Effects.CastFromCollection(card),
                    descriptionOverride = "You may cast target card with the same name as that spell " +
                        "from your graveyard.",
                )
            )
        }
        description = "Whenever you cast an instant or sorcery spell from your hand, you may cast " +
            "target card with the same name as that spell from your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Raymond Swanland"
        flavorText = "\"There is no place like a rooftop laboratory in a lightning storm. It's where " +
            "genius strikes.\"\n—Renna, Selhoff alchemist"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/5294d359-c599-40ed-9e06-2a3cc8624d6a.jpg?1783937750"
        ruling("2016-04-08", "You must choose a target for Harness the Storm's ability (if one exists) immediately after you've cast an instant or sorcery spell. There's no way for that spell to be countered and then targeted by Harness the Storm once it's in the graveyard.")
        ruling("2016-04-08", "You choose whether to cast the targeted card as the ability resolves. You can't wait to cast it later, and there's no way for your opponents to set up a situation where you have to cast the card but don't want to.")
        ruling("2016-04-08", "If the spell you cast from your hand is countered in response to Harness the Storm's triggered ability, you can still cast the target card while the ability is resolving.")
        ruling("2016-04-08", "Because you're paying the spell's costs, you can pay alternative costs, such as awaken costs and surge costs from the Battle for Zendikar block. You can also pay additional costs, such as kicker costs. If the card has a mandatory additional cost, like Lightning Axe, you must pay it to cast the card.")
        ruling("2016-04-08", "The spell you cast from your graveyard can target the spell you cast from your hand.")
        ruling("2016-04-08", "The spell you cast from your graveyard resolves before the spell you cast from your hand. It's put into your graveyard as it resolves.")
    }
}
