package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Bramble Familiar // Fetch Quest
 * {1}{G}
 * Creature — Elemental Raccoon
 * 2/2
 * {T}: Add {G}.
 * {1}{G}, {T}, Discard a card: Return this creature to its owner's hand.
 *
 * Adventure: Fetch Quest — {5}{G}{G}, Sorcery — Adventure
 * Mill seven cards. Then put a creature, enchantment, or land card from among the milled
 * cards onto the battlefield.
 *
 * The bounce ability returns the *source* to hand, so it takes [EffectTarget.Self] rather than a
 * target — the creature can rebuy the Adventure by going back to hand and being recast later.
 * Its cost is the literal three-atom composite (mana + tap + discard); the discard is a cost, not
 * an effect, so it happens on activation even if the ability is later countered.
 *
 * The Adventure is the standard Gather → Move → Select → Move mill-and-retrieve pipeline (see
 * Picklock Prankster), with `isMill = true` on the gather so mill-watchers see a real mill event,
 * and a battlefield destination instead of hand. The cards hit the graveyard *first* and the
 * selection reads the `fetch_milled` collection afterwards — that ordering is what makes "from
 * among the milled cards" mean those specific seven rather than "anything in your graveyard".
 *
 * Oracle says "Then put ... onto the battlefield", not "you may put", so this is
 * [SelectionMode.ChooseExactly]`(1)`. The executor selects nothing when the eligible pool is empty
 * (all seven milled cards were instants/sorceries), so the mandatory wording degrades correctly
 * instead of deadlocking. `showAllCards = true` keeps the ineligible milled cards visible so the
 * player can see what the mill actually hit.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val BrambleFamiliar = card("Bramble Familiar") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Raccoon"
    oracleText = "{T}: Add {G}.\n{1}{G}, {T}, Discard a card: Return this creature to its owner's hand."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    adventure("Fetch Quest") {
        manaCost = "{5}{G}{G}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Mill seven cards. Then put a creature, enchantment, or land card from among " +
            "the milled cards onto the battlefield. " +
            "(Then exile this card. You may cast the creature later from exile.)"

        spell {
            effect = Effects.Composite(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(
                            count = DynamicAmount.Fixed(7),
                            player = Player.You,
                            isMill = true,
                        ),
                        storeAs = "fetch_milled",
                    ),
                    MoveCollectionEffect(
                        from = "fetch_milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                    ),
                    SelectFromCollectionEffect(
                        from = "fetch_milled",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        filter = GameObjectFilter(
                            cardPredicates = listOf(
                                CardPredicate.Or(
                                    listOf(
                                        CardPredicate.IsCreature,
                                        CardPredicate.IsEnchantment,
                                        CardPredicate.IsLand,
                                    ),
                                ),
                            ),
                        ),
                        storeSelected = "fetch_selected",
                        showAllCards = true,
                        prompt = "Put a creature, enchantment, or land card onto the battlefield",
                        selectedLabel = "Put onto the battlefield",
                        remainderLabel = "Leave in graveyard",
                    ),
                    MoveCollectionEffect(
                        from = "fetch_selected",
                        destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You),
                    ),
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "164"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/475d7e9a-759d-4523-a5cd-2a6e0d1b14ea.jpg?1783915084"
    }
}
