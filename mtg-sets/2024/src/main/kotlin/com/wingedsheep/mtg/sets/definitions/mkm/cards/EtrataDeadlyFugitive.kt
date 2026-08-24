package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Etrata, Deadly Fugitive — Murders at Karlov Manor #200
 * {1}{U}{B} · Legendary Creature — Vampire Assassin · 1/4
 *
 * Deathtouch
 * Face-down creatures you control have "{2}{U}{B}: Turn this creature face up. If you can't, exile
 * it, then you may cast the exiled card without paying its mana cost."
 * Whenever an Assassin you control deals combat damage to an opponent, cloak the top card of that
 * player's library.
 *
 * The two halves are one engine: the trigger turns opponents' libraries into face-down 2/2s under
 * your control, and the granted ability turns each of those into either a real permanent or a free
 * spell. Etrata is an Assassin herself, so her own connection starts the loop.
 *
 * **"If you can't" is the gated action's failure branch, not a condition.** The granted ability's
 * primary instruction *is* the turn-up attempt, so it rides `Gate.DoAction` with the new
 * [SuccessCriterion.TurnedFaceUp], which reads whether a `TurnFaceUpEvent` was actually emitted.
 * Encoding it as a "can this be turned face up?" condition instead would mean re-deriving the
 * engine's own turn-up legality in a second place and letting the two drift. `ifYouDo` is an empty
 * composite — the house spelling for a gate that only has a failure branch (Kellan, the Kid).
 *
 * The two ways it can fail, per the printed ruling: the permanent is a *cloaked or manifested
 * instant or sorcery card*, which CR 701.58g / 701.40g say is revealed and left face down (and, the
 * same rules add, doesn't trigger "turned face up" abilities); or an effect prohibits turning
 * face-down creatures face up at all. A morph/disguise face-down permanent is always a creature
 * card, so for those the ability is simply a second, more expensive turn-up price.
 *
 * **The grant covers every face-down creature you control**, not just the ones Etrata cloaked:
 * `GroupFilter(Creature.youControl().faceDown())`. Your opponent's face-down creatures don't get
 * it, and a face-down permanent that isn't a creature (there is no such thing under current rules —
 * face-down permanents enter as 2/2 creatures) is excluded by construction. `EffectTarget.Self`
 * inside a granted ability resolves to the *host* that received it, so both the turn-up and the
 * exile act on the face-down creature being activated, never on Etrata.
 *
 * **The exiled card goes to its owner's exile and is cast by you.** A cloaked card is usually owned
 * by the opponent whose library it came from; `MoveCollectionExecutor` routes a battlefield→exile
 * move to the owner's exile zone regardless of the nominal destination player, and the free cast is
 * `Chooser.Controller` — you cast a card you don't own, which is the whole point of the card.
 *
 * **The cloak trigger reads the damaged player, not the attacker's defending player.** An
 * ANY-bound `dealsDamage` trigger with a `sourceFilter` binds the damaging creature as the
 * triggering entity and the damaged player as [Player.TriggeringPlayer] — so "that player's
 * library" is the player who actually took the damage, which matters when several Assassins connect
 * with different opponents in the same combat damage step (each is its own trigger). The cloaked
 * card enters the battlefield under *your* control (`underOwnersControl` left false), matching the
 * ruling that the cards you cloaked are exiled if you leave the game.
 */
val EtrataDeadlyFugitive = card("Etrata, Deadly Fugitive") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Vampire Assassin"
    oracleText = "Deathtouch\n" +
        "Face-down creatures you control have \"{2}{U}{B}: Turn this creature face up. If you " +
        "can't, exile it, then you may cast the exiled card without paying its mana cost.\"\n" +
        "Whenever an Assassin you control deals combat damage to an opponent, cloak the top card " +
        "of that player's library. (To cloak a card, put it onto the battlefield face down as a " +
        "2/2 creature with ward {2}. Turn it face up any time for its mana cost if it's a " +
        "creature card.)"
    power = 1
    toughness = 4
    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Mana("{2}{U}{B}"),
                effect = IfYouDoEffect(
                    action = TurnFaceUpEffect(EffectTarget.Self),
                    ifYouDo = Effects.Composite(emptyList()),
                    ifYouDont = Effects.Pipeline {
                        val thisCreature = gather(CardSource.Self)
                        val exiled = moveTracked(
                            thisCreature,
                            CardDestination.ToZone(Zone.EXILE),
                            name = "etrataExiled",
                        )
                        run(
                            MayEffect(
                                Effects.CastFromCollectionWithoutPayingCost(exiled.key),
                                descriptionOverride = "You may cast the exiled card without " +
                                    "paying its mana cost.",
                            )
                        )
                    },
                    successCriterion = SuccessCriterion.TurnedFaceUp,
                    descriptionOverride = "Turn this creature face up. If you can't, exile it, " +
                        "then you may cast the exiled card without paying its mana cost.",
                ),
                descriptionOverride = "Turn this creature face up. If you can't, exile it, " +
                    "then you may cast the exiled card without paying its mana cost.",
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl().faceDown()),
        )
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            recipient = RecipientFilter.Opponent,
            sourceFilter = GameObjectFilter.Creature.withSubtype(Subtype.ASSASSIN).youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    count = DynamicAmount.Fixed(1),
                    player = Player.TriggeringPlayer,
                ),
                storeAs = "etrataCloaked",
                lookAudience = LookAudience.None,
            ),
            MoveCollectionEffect(
                from = "etrataCloaked",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                faceDown = FaceDownMode.CLOAK,
            ),
        )
        description = "Whenever an Assassin you control deals combat damage to an opponent, " +
            "cloak the top card of that player's library."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "200"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4410db5a-62af-43ac-979d-88a7c975f7bd.jpg?1783912850"

        ruling(
            "2024-02-02",
            "You might be unable to turn a face-down creature face up because it's an instant or " +
                "sorcery. Alternatively, abilities such as that of Karlov Watchdog might prevent " +
                "you from turning face-down creatures face up altogether. In those cases, you'll " +
                "exile that creature, and then you'll choose whether or not to cast that card " +
                "without paying its mana cost."
        )
        ruling(
            "2024-02-02",
            "If you cast a spell \"without paying its mana cost\", you can't choose to cast it " +
                "for any alternative costs. You can, however, pay additional costs, such as " +
                "kicker costs. If the card has any mandatory additional costs, such as that of " +
                "Demand Answers, those must be paid to cast the spell."
        )
        ruling(
            "2024-02-02",
            "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X " +
                "when casting it without paying its mana cost."
        )
        ruling("2024-02-02", "Your opponents can't look at cards they own that you cloaked.")
        ruling(
            "2024-02-02",
            "In a multiplayer game, if an opponent leaves the game, all of the cards they own " +
                "that you cloaked leave as well. If you leave the game, the creatures you cloaked " +
                "with Etrata, Deadly Fugitive's triggered ability are exiled."
        )
        ruling(
            "2024-02-02",
            "To cloak a card, put it onto the battlefield face down. It becomes a 2/2 face-down " +
                "creature card with ward {2} and no name, mana cost, or creature types. It's " +
                "colorless and has a mana value of 0. Other effects that apply to the permanent " +
                "can still grant it any characteristics it doesn't have or change the " +
                "characteristics it does have."
        )
        ruling(
            "2024-02-02",
            "If something tries to turn a face-down instant or sorcery card on the battlefield " +
                "face up, reveal that card to show all players it's an instant or sorcery card. " +
                "The permanent remains on the battlefield face down. Abilities that trigger when " +
                "a permanent turns face up won't trigger, because even though you revealed the " +
                "card, it never turned face up."
        )
    }
}
