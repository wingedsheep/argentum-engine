package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blight Herder
 * {5}
 * Creature — Eldrazi Processor
 * 4/5
 * When you cast this spell, you may put two cards your opponents own from exile into their owners'
 * graveyards. If you do, create three 1/1 colorless Eldrazi Scion creature tokens. They have
 * "Sacrifice this token: Add {C}."
 *
 * A cast trigger (`Triggers.self.isCast()`): it resolves before Blight Herder, so the Scions
 * arrive even if the creature spell is countered.
 *
 * "Two" is all-or-nothing — you may not put just one. The shape is the engine's "you may [choose
 * exactly N from a pool]. If you do, …" idiom: `Effects.May` over an `IfYouDo` whose action picks
 * *exactly* two from every opponent's exile and whose bar is `min = 2`. With fewer than two cards
 * your opponents own in exile, "yes" can't clear that bar, so the option is unavailable and the
 * prompt is skipped (CR 608.2d); with two or more, "yes" forces a pick of exactly two, which may
 * come from different opponents (2015-08-25 ruling). Each card goes to its owner's graveyard.
 */
val BlightHerder = card("Blight Herder") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi Processor"
    power = 4
    toughness = 5
    oracleText = "When you cast this spell, you may put two cards your opponents own from exile into their " +
        "owners' graveyards. If you do, create three 1/1 colorless Eldrazi Scion creature tokens. They have " +
        "\"Sacrifice this token: Add {C}.\""

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val opponentsExile = gather(
                        CardSource.FromZone(zone = Zone.EXILE, player = Player.EachOpponent, filter = GameObjectFilter.Any)
                    )
                    // Named: the "if you do" check below reads it from outside the pipeline.
                    val processed = chooseExactly(
                        2,
                        from = opponentsExile,
                        prompt = "Choose two cards your opponents own from exile to put into their owners' graveyards",
                        selectedLabel = "Put into graveyard",
                        name = "processed",
                    )
                    toGraveyard(processed)
                },
                then = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = emptySet(),
                    creatureTypes = setOf("Eldrazi", "Scion"),
                    count = 3,
                    activatedAbilities = listOf(
                        ActivatedAbility(
                            cost = Costs.SacrificeSelf,
                            effect = Effects.AddColorlessMana(1),
                            isManaAbility = true,
                            timing = TimingRule.ManaAbility,
                            descriptionOverride = "Sacrifice this token: Add {C}.",
                        )
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/b/9/b999a0fe-d2d0-4367-9abb-6ce5f3764f19.jpg?1783938121",
                ),
                successCriterion = SuccessCriterion.CollectionNonEmpty("processed", min = 2),
            ),
            prompt = "Put two cards your opponents own from exile into their owners' graveyards?",
        )
        description = "When you cast this spell, you may put two cards your opponents own from exile into " +
            "their owners' graveyards. If you do, create three 1/1 colorless Eldrazi Scion creature tokens. " +
            "They have \"Sacrifice this token: Add {C}.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Todd Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/478c3a74-ff78-4041-8137-93356d370ece.jpg?1783938225"
        ruling("2015-08-25", "If a spell or ability requires that you put more than one exiled card into the graveyard, you may choose cards owned by different opponents. Each card chosen will be put into its owner's graveyard.")
        ruling("2015-08-25", "If a replacement effect will cause cards that would be put into a graveyard from anywhere to be exiled instead (such as the one created by Anafenza, the Foremost), you can still put an exiled card into its opponent's graveyard. The card becomes a new object and remains in exile. In this situation, you can't use a single exiled card if required to put more than one exiled card into the graveyard. Conversely, you could use the same card in this situation if two separate spells or abilities each required you to put a single exiled card into its owner's graveyard.")
        ruling("2015-08-25", "You can't look at face-down cards in exile unless an effect allows you to.")
        ruling("2015-08-25", "Face-down cards in exile are grouped using two criteria: what caused them to be exiled face down and when they were exiled face down. If you want to put a face-down card in exile into its owner's graveyard, you must first choose one of these groups and then choose a card from within that group at random. For example, say an artifact causes your opponent to exile their hand of three cards face down. Then on a later turn, that artifact causes your opponent to exile another two cards face down. If you use Wasteland Strangler to put one of those cards into their graveyard, you would pick the first or second pile and put a card chosen at random from that pile into the graveyard.")
        ruling("2015-08-25", "Eldrazi Scions are similar to Eldrazi Spawn, seen in the Zendikar block. Note that Eldrazi Scions are 1/1, not 0/1.")
        ruling("2015-08-25", "Eldrazi and Scion are each separate creature types. Anything that affects Eldrazi will affect these tokens, for example.")
        ruling("2015-08-25", "Sacrificing an Eldrazi Scion creature token to add {C} is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}
