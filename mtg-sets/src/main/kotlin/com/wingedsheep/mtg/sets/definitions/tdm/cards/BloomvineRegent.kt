package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bloomvine Regent // Claim Territory
 * {3}{G}{G} // {2}{G}
 * Creature — Dragon // Sorcery — Omen
 * 4/5
 *
 * Bloomvine Regent:
 *   Flying
 *   Whenever this creature or another Dragon you control enters, you gain 3 life.
 *
 * Claim Territory — {2}{G}, Sorcery — Omen:
 *   Search your library for up to two basic Forest cards, reveal them, put one onto the
 *   battlefield tapped and the other into your hand, then shuffle. (Also shuffle this card.)
 *
 * The ETB life-gain uses an ANY binding so it fires for the Regent itself as well as for any
 * other Dragon you control entering. Claim Territory is a Cultivate-style "search up to two
 * basics" split (one to battlefield tapped, one to hand) — the [SelectionMode.ChooseExactly]
 * split step auto-selects the only card when one basic is found. The "(Also shuffle this card.)"
 * reminder is handled by the Omen/Adventure exile-and-recast flow.
 */
val BloomvineRegent = card("Bloomvine Regent") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever this creature or another Dragon you control enters, you gain 3 life."

    keywords(Keyword.FLYING)

    // Whenever this creature or another Dragon you control enters, you gain 3 life.
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON),
            binding = TriggerBinding.ANY
        )
        effect = Effects.GainLife(3)
        description = "Whenever this creature or another Dragon you control enters, you gain 3 life."
    }

    // Claim Territory — Omen. Search your library for up to two basic Forest cards, reveal them,
    // put one onto the battlefield tapped and the other into your hand, then shuffle.
    adventure("Claim Territory") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery — Omen"
        oracleText = "Search your library for up to two basic Forest cards, reveal them, put one " +
            "onto the battlefield tapped and the other into your hand, then shuffle. " +
            "(Also shuffle this card.)"
        spell {
            effect = Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(
                        Zone.LIBRARY,
                        Player.You,
                        GameObjectFilter.BasicLand.withSubtype(Subtype.FOREST)
                    ),
                    name = "searchable"
                )
                val found = chooseUpTo(
                    2, from = searchable,
                    prompt = "Search your library for up to two basic Forest cards",
                    name = "found"
                )
                val (toBattlefield, toHand) = chooseExactlySplit(
                    1, from = found,
                    selectedLabel = "Onto the battlefield tapped",
                    remainderLabel = "Into your hand",
                    prompt = "Choose which basic Forest enters the battlefield tapped; the other goes to your hand.",
                    name = "toBattlefield",
                    remainderName = "toHand"
                )
                move(
                    toBattlefield,
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    revealed = true
                )
                move(
                    toHand,
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                )
                run(ShuffleLibraryEffect())
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10e0a9a3-f63a-4f92-a083-9d181580e498.jpg?1754359377"
    }
}
