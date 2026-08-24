package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulbond
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tandem Lookout
 * {2}{U}
 * Creature — Human Scout
 * 2/1
 * Soulbond (You may pair this creature with another unpaired creature when either enters. They
 * remain paired for as long as you control both of them.)
 * As long as Tandem Lookout is paired with another creature, each of those creatures has
 * "Whenever this creature deals damage to an opponent, draw a card."
 *
 * [DeadeyeNavigator]'s shape with a triggered ability instead of an activated one:
 * [GrantTriggeredAbility] over `GroupFilter.soulbondPair()` — the Lookout *and* its partner, and
 * nobody at all while unpaired (CR 702.95b/e).
 *
 * The trigger is [DamageType.Any], not combat only — the printed line says "deals damage", so a
 * partner that pings an opponent with an activated ability draws too. [RecipientFilter.Opponent]
 * resolves against the *host* creature's controller, which is the same player for both halves of a
 * soulbond pair (CR 702.95a requires you control both), so either half firing draws for you.
 *
 * `TriggerBinding.SELF` is what makes "this creature" mean the half that dealt the damage rather
 * than the Lookout: the engine hosts a granted trigger on the *receiving* permanent (CR 113.7), so
 * each creature's copy watches its own damage.
 */
val TandemLookout = card("Tandem Lookout") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Scout"
    oracleText = "Soulbond (You may pair this creature with another unpaired creature when either " +
        "enters. They remain paired for as long as you control both of them.)\n" +
        "As long as Tandem Lookout is paired with another creature, each of those creatures has " +
        "\"Whenever this creature deals damage to an opponent, draw a card.\""
    power = 2
    toughness = 1

    soulbond()

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.dealsDamage(recipient = RecipientFilter.Opponent).event,
                binding = Triggers.dealsDamage(recipient = RecipientFilter.Opponent).binding,
                effect = Effects.DrawCards(1),
                descriptionOverride = "Whenever this creature deals damage to an opponent, draw a card"
            ),
            filter = GroupFilter.soulbondPair()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83564e67-2677-4955-a3b9-3b221dbb100b.jpg?1783940709"
        ruling("2017-03-14", "If Tandem Lookout or the creature it's paired with is dealt lethal damage at the same time that either deals damage to an opponent, its ability triggers. You'll draw a card even though that creature no longer has the ability.")
    }
}
