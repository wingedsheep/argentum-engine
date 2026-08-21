package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kylox's Voltstrider — Murders at Karlov Manor #215
 * {1}{U}{R} · Artifact — Vehicle · 4/4
 *
 * Collect evidence 6: This Vehicle becomes an artifact creature until end of turn.
 * Whenever this Vehicle attacks, you may cast an instant or sorcery spell from among cards exiled
 * with it. If that spell would be put into a graveyard, put it on the bottom of its owner's
 * library instead.
 * Crew 2
 *
 * The whole card is one idea — the collect-evidence payment *is* the fuel for the attack trigger —
 * and the seam between the two is the only part that needed engine work. Collect evidence
 * (CR 701.59) ordinarily exiles the cards and forgets them; `linkToSource` tethers them to this
 * Vehicle's linked-exile pile instead, which is what makes "cards exiled **with it**" a set the
 * attack trigger can name via [CardSource.FromLinkedExile]. Every other collect-evidence card in
 * the corpus leaves the flag off, so no existing pile changes shape.
 *
 * The pile is *cumulative*: each activation adds to it, and it prunes itself. A card that leaves
 * exile is dropped from every linked-exile pile by
 * `ZoneMovementUtils.unlinkFromAllLinkedExiles`, and
 * [com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExileLookup] filters the survivors
 * by "still in exile" a second time — so a spell cast off a previous attack, now on the bottom of
 * a library, is out of the pool without this card doing any bookkeeping.
 *
 * `ChooseUpTo(1)` is the "you may": declining is a legal, silent outcome, and the cards stay
 * exiled for a later attack. The cast pays the spell's **normal** mana cost — the card conspicuously
 * does not say "without paying its mana cost" — so this is [Effects.CastFromCollection], not its
 * free sibling. Per the 2024-02-02 ruling the cast happens during the trigger's own resolution,
 * which is exactly what that effect does; timing restrictions on a sorcery are therefore ignored.
 *
 * `insteadOfGraveyard` is the printed rider. It stamps the one card being cast so that when the
 * spell leaves the stack for its owner's graveyard — resolved, countered or fizzled alike — it
 * goes to the bottom of that library instead. Scoping it to the cast card rather than to the pile
 * matters: a card left uncast keeps no marker, so casting it later by any other route behaves
 * normally.
 *
 * Crew 2 is untouched vocabulary, and the animate ability is the same
 * [Effects.BecomeCreature] shape crew itself builds — a Vehicle is already an artifact, so only
 * the creature type needs adding.
 */
val KyloxsVoltstrider = card("Kylox's Voltstrider") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Collect evidence 6: This Vehicle becomes an artifact creature until end of turn.\n" +
        "Whenever this Vehicle attacks, you may cast an instant or sorcery spell from among cards " +
        "exiled with it. If that spell would be put into a graveyard, put it on the bottom of its " +
        "owner's library instead.\n" +
        "Crew 2"

    activatedAbility {
        cost = Costs.CollectEvidence(6, linkToSource = true)
        effect = Effects.BecomeCreature(EffectTarget.Self, power = 4, toughness = 4)
        description = "This Vehicle becomes an artifact creature until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromLinkedExile(),
                    storeAs = "exiledWithIt"
                ),
                SelectFromCollectionEffect(
                    from = "exiledWithIt",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.InstantOrSorcery,
                    storeSelected = "toCast",
                    // No `showAllCards`: the pile is public exile the player can already read, and
                    // an all-permanents pile would otherwise raise a picker with nothing in it
                    // selectable and no way out.
                    prompt = "Cast an instant or sorcery exiled with Kylox's Voltstrider?"
                ),
                Effects.CastFromCollection(
                    from = "toCast",
                    insteadOfGraveyard = AfterResolveDestination.BOTTOM_OF_LIBRARY
                )
            )
        )
        description = "Whenever this Vehicle attacks, you may cast an instant or sorcery spell " +
            "from among cards exiled with it. If that spell would be put into a graveyard, put " +
            "it on the bottom of its owner's library instead."
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 2))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86a8a1af-b1cf-47fc-ab42-7efa07a1c95b.jpg?1783912844"

        ruling(
            "2024-02-02",
            "You choose whether or not to cast a spell from among the exiled cards as Kylox's " +
                "Voltstrider's triggered ability resolves. If you do, you do so as part of the " +
                "resolution of that ability. You can't wait to cast it later in the turn. Timing " +
                "restrictions based on the card's type are ignored."
        )
        ruling(
            "2024-02-02",
            "If you can't exile enough cards to meet or exceed the required mana value, you " +
                "can't choose to collect evidence at all."
        )
        ruling(
            "2024-02-02",
            "Once you've announced that you're casting a spell, players can't take actions until " +
                "you've finished doing so. Notably, opponents can't try to remove cards from your " +
                "graveyard to stop you from collecting evidence."
        )
    }
}
