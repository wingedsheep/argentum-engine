package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaya the Inexorable
 * {3}{W}{B}
 * Legendary Planeswalker — Kaya
 * Loyalty 5
 *
 * +1: Put a ghostform counter on up to one target nontoken creature. It gains "When this creature
 * dies or is put into exile, return it to its owner's hand and create a 1/1 white Spirit creature
 * token with flying."
 * −3: Exile target nonland permanent.
 * −7: You get an emblem with "At the beginning of your upkeep, you may cast a legendary spell from
 * your hand, from your graveyard, or from among cards you own in exile without paying its mana cost."
 *
 * Modeling notes:
 *  - The ghostform counter is only a reminder (ruling): the granted ability is a separate
 *    `Duration.Permanent` grant, so removing the counter doesn't stop it. "Dies or is put into
 *    exile" is two granted triggers (the Ashnod's Intervention shape) — one per destination, each
 *    returning the card from that zone and making the Spirit. Each Kaya activation grants its own
 *    pair, so two activations make two Spirits but one return (ruling).
 *  - The emblem is a permanent global triggered ability (the Kaito / Sephiroth shape). It gathers
 *    your legendary nonland cards from hand, graveyard and exile (exile is keyed by owner, so "cards
 *    you own in exile"), lets you pick up to one, and casts it without paying its mana cost during
 *    the resolution (CR 608.2g — timing permissions are ignored; X is 0, mandatory additional costs
 *    are still paid).
 */
val KayaTheInexorable = card("Kaya the Inexorable") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Kaya"
    startingLoyalty = 5
    oracleText = "+1: Put a ghostform counter on up to one target nontoken creature. It gains \"When " +
        "this creature dies or is put into exile, return it to its owner's hand and create a 1/1 white " +
        "Spirit creature token with flying.\"\n" +
        "−3: Exile target nonland permanent.\n" +
        "−7: You get an emblem with \"At the beginning of your upkeep, you may cast a legendary spell " +
        "from your hand, from your graveyard, or from among cards you own in exile without paying its " +
        "mana cost.\""

    val spirit = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.WHITE),
        creatureTypes = setOf("Spirit"),
        keywords = setOf(Keyword.FLYING),
    )
    val ghostformText = "When this creature dies or is put into exile, return it to its owner's hand " +
        "and create a 1/1 white Spirit creature token with flying."

    // +1: Put a ghostform counter on up to one target nontoken creature. It gains the return trigger.
    loyaltyAbility(+1) {
        val creature = target(TargetFilter(GameObjectFilter.Creature.nontoken()), optional = true)
        effect = Effects.AddCounters(CounterType.GHOSTFORM, 1, creature) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.dies(),
                    effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD) then spirit,
                    descriptionOverride = ghostformText,
                ),
                target = creature,
                duration = Duration.Permanent,
            ) then
            Effects.GrantTriggeredAbility(
                ability = TriggeredAbility.create(
                    trigger = Triggers.self.leaves(to = Zone.EXILE),
                    effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.EXILE) then spirit,
                    descriptionOverride = ghostformText,
                ),
                target = creature,
                duration = Duration.Permanent,
            )
        description = "Put a ghostform counter on up to one target nontoken creature. It gains \"$ghostformText\""
    }

    // −3: Exile target nonland permanent.
    loyaltyAbility(-3) {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(permanent)
    }

    // −7: Emblem — at the beginning of your upkeep, you may cast a legendary spell from your hand,
    //     graveyard, or from among cards you own in exile without paying its mana cost.
    val emblemText = "At the beginning of your upkeep, you may cast a legendary spell from your hand, " +
        "from your graveyard, or from among cards you own in exile without paying its mana cost."
    loyaltyAbility(-7) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.Pipeline {
                    val pool = gather(
                        CardSource.FromMultipleZones(
                            listOf(Zone.HAND, Zone.GRAVEYARD, Zone.EXILE),
                            Player.You,
                            GameObjectFilter.Nonland.legendary(),
                        )
                    )
                    val spell = chooseUpTo(
                        1,
                        from = pool,
                        prompt = "You may cast a legendary spell without paying its mana cost",
                        selectedLabel = "Cast for free",
                    )
                    run(Effects.CastFromCollectionWithoutPayingCost(spell))
                },
                descriptionOverride = emblemText,
            ),
            descriptionOverride = emblemText,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "218"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/1/6/166c0bd1-b59d-4644-832f-888a4ca3a0aa.jpg?1783928194"

        ruling("2021-02-05", "The ghostform counter added by the first ability doesn't have any functional importance. It's there to remind you of the ability the creature gains. Even if that counter is removed from the creature, the triggered ability it gains will still work.")
        ruling("2021-02-05", "If a nontoken creature gains more than one instance of that triggered ability, each of them will trigger separately. You can return the card to its owner's hand only once, but you'll create a Spirit creature token for each of those abilities.")
        ruling("2021-02-05", "If you own a face-down card in exile, Kaya's emblem will let you cast it only if you can look at the card (and only if it's legendary). If you aren't allowed to look at the card, you can't cast it, even if you know it's legendary.")
        ruling("2021-02-05", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2021-02-05", "If the spell has {X} in its mana cost, you must choose 0 as the value of X.")
    }
}
