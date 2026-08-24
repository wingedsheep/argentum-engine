package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * A Killer Among Us — Murders at Karlov Manor #167
 * {4}{G} · Enchantment · Uncommon
 *
 * When this enchantment enters, create a 1/1 white Human creature token, a 1/1 blue Merfolk
 * creature token, and a 1/1 red Goblin creature token. Then secretly choose Human, Merfolk, or
 * Goblin.
 * Sacrifice this enchantment, Reveal the creature type you chose: If target attacking creature
 * token is the chosen type, put three +1/+1 counters on it and it gains deathtouch until end of
 * turn.
 *
 * The set's murder mystery in one card: three identical-looking suspects, and only you know which
 * one is the killer. The bluff is the card — an opponent who has to block all three the same way
 * is the whole point — so the *secrecy* is a rules requirement, not flavour text.
 *
 * `Effects.SecretlyChooseCreatureType(...)` is the hidden-agenda primitive (CR 702.106a-b — a
 * choice noted on a piece of paper kept with the object) applied to a permanent: the type is noted
 * on this enchantment's `NotedCreatureTypesComponent`, stamped with the chooser's id, and shown in
 * the client only to them. `Costs.RevealNotedCreatureType` is the other half. Three consequences
 * that are all card behaviour rather than plumbing:
 *
 *  - **Only the chooser can activate the ability.** A player who gains control of the enchantment
 *    never saw the choice, so the reveal cost is unpayable for them and the ability isn't offered
 *    at all — exactly what the card's ruling says.
 *  - **The chosen type survives the sacrifice.** Both cost pieces are paid together (CR 601.2h),
 *    so the enchantment and its note are in the graveyard by the time the ability resolves. The
 *    type is captured at activation as last-known information (CR 113.7a) and handed to the effect
 *    as `chosenValues["chosenCreatureType"]`.
 *  - **Any attacking creature token is a legal target.** Not just one of the three this card made,
 *    and not just one of the chosen type — the type test is a condition on the *effect*, so a
 *    wrong-type target is targeted legally and simply gets nothing. That also makes the activation
 *    a bluff an opponent can be baited into misreading.
 *
 * The type test is an ordinary filter read: `withSubtypeFromVariable("chosenCreatureType")` over
 * the revealed value, which means a changeling attacking creature token is every creature type
 * (CR 702.73a) and always qualifies.
 */
val AKillerAmongUs = card("A Killer Among Us") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 white Human creature token, a 1/1 " +
        "blue Merfolk creature token, and a 1/1 red Goblin creature token. Then secretly choose " +
        "Human, Merfolk, or Goblin.\n" +
        "Sacrifice this enchantment, Reveal the creature type you chose: If target attacking " +
        "creature token is the chosen type, put three +1/+1 counters on it and it gains " +
        "deathtouch until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Human"),
                name = "Human",
                imageUri = "https://cards.scryfall.io/normal/front/d/7/d7cd1de1-9657-4262-be11-8279b3408e54.jpg?1783912610"
            ),
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLUE),
                creatureTypes = setOf("Merfolk"),
                name = "Merfolk",
                imageUri = "https://cards.scryfall.io/normal/front/c/6/c63ce61b-480a-4da6-80f6-63e096902ae6.jpg?1783912609"
            ),
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Goblin"),
                name = "Goblin",
                imageUri = "https://cards.scryfall.io/normal/front/c/d/cd6cd0d3-7973-49e6-9c1c-6f516a5d5fe5.jpg?1783912608"
            ),
            // "Then" — the choice happens after the tokens exist, so the player picks knowing the
            // board. The three types are the only options; the note is secret to the chooser.
            Effects.SecretlyChooseCreatureType(
                options = listOf("Human", "Merfolk", "Goblin"),
                prompt = "Secretly choose Human, Merfolk, or Goblin"
            )
        )
        description = "When this enchantment enters, create a 1/1 white Human creature token, a " +
            "1/1 blue Merfolk creature token, and a 1/1 red Goblin creature token. Then secretly " +
            "choose Human, Merfolk, or Goblin."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.SacrificeSelf, Costs.RevealNotedCreatureType)
        target = TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.attacking().token()),
            id = "target attacking creature token"
        )
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter.Creature.withSubtypeFromVariable("chosenCreatureType")
            ),
            effect = Effects.Composite(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.ContextTarget(0)),
                Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.ContextTarget(0))
            )
        )
        description = "Sacrifice this enchantment, Reveal the creature type you chose: If target " +
            "attacking creature token is the chosen type, put three +1/+1 counters on it and it " +
            "gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c1392c5-91a5-4e6e-803d-ed032e4d594b.jpg?1783912865"

        ruling(
            "2024-02-09",
            "A Killer Among Us's last ability can target any attacking creature token, not just " +
                "one of the tokens created by its first ability, and not just one of the chosen " +
                "type. Only a token of the chosen type will get the bonuses, though."
        )
        ruling(
            "2024-02-09",
            "Only the player who secretly chose the creature type can reveal the creature type " +
                "they chose. If another player gains control of A Killer Among Us, they will be " +
                "unable to activate its last ability."
        )
        ruling(
            "2024-02-09",
            "If you control multiple copies of A Killer Among Us, you may choose the same or " +
                "different creature types for each one."
        )
    }
}
