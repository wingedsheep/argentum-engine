package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The token band: the count, the colour run, the keyword rider, and the predefined nouns.
 *
 * Two properties are worth asserting beyond the round trips. The colour and keyword *sets* have no
 * order of their own, so printing has to impose one and it has to be Magic's — WUBRG for colours,
 * and the same declaration order for keywords; a card that stored them the other way round must
 * print the same sentence rather than a different one. And a predefined token is a noun the SDK
 * names, so it must build the value the facade builds and not a spelled-out token that happens to
 * look like one.
 */
class TokensTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    "the count is a slot, and the noun's number moves with it" {
        fragment("Create a 1/1 white Rabbit creature token.").script.spellEffect shouldBe
            Effects.CreateToken(power = 1, toughness = 1, colors = setOf(Color.WHITE), creatureTypes = setOf("Rabbit"))
        (fragment("Create two 1/1 white Rabbit creature tokens.").script.spellEffect as CreateTokenEffect)
            .count shouldBe DynamicAmount.Fixed(2)

        roundTrips("Create a 1/1 white Rabbit creature token.")
        roundTrips("Create two 1/1 white Rabbit creature tokens.")
        roundTrips("Create three 2/2 black Zombie creature tokens.")
        roundTrips("Create X 1/1 green Saproling creature tokens.")
    }

    // Otterball Antics and Stormchaser's Talent — a two-colour token with a keyword.
    "a token carries a colour run and a keyword run" {
        fragment("Create a 1/1 blue and red Otter creature token with prowess.").script.spellEffect shouldBe
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLUE, Color.RED),
                creatureTypes = setOf("Otter"),
                keywords = setOf(Keyword.PROWESS),
            )

        roundTrips("Create a 1/1 blue and red Otter creature token with prowess.")
        roundTrips("Create a 0/4 white Wall creature token with defender.")
        roundTrips("Create a 5/5 red Dragon creature token with flying.")
        roundTrips("Create a 7/7 green Elemental creature token with trample.")
        roundTrips("Create a 1/1 colorless Sliver creature token.")
    }

    // Ornithopter-makers and Glimmers — the card types in front of "creature" are one slot, crossing
    // the count, the tapped entry and the keyword rider.
    "an artifact or enchantment creature token is a slot over the noun" {
        fragment("Create a 1/1 colorless Thopter artifact creature token with flying.").script.spellEffect shouldBe
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Thopter"),
                keywords = setOf(Keyword.FLYING),
                artifactToken = true,
            )
        (fragment("Create a 1/1 white Glimmer enchantment creature token.").script.spellEffect as CreateTokenEffect)
            .enchantmentToken shouldBe true

        roundTrips("Create a 1/1 colorless Thopter artifact creature token with flying.")
        roundTrips("Create two 1/1 colorless Servo artifact creature tokens.")
        roundTrips("Create a tapped 2/2 colorless Robot artifact creature token.")
        roundTrips("Create a 1/1 white Glimmer enchantment creature token.")
    }

    // Both kinds at once is printed once in Oracle, in the order opposite to the type line's — no
    // row spells it, so it refuses to print rather than guess.
    "an artifact enchantment creature token refuses to print" {
        val both = CardFragment(
            script = CardScript(
                spellEffect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Thopter"),
                    artifactToken = true,
                    enchantmentToken = true,
                ),
            ),
        )
        Grammar.abilityLine.printLine(both) shouldBe null
    }

    // The sets have no order; the printer imposes WUBRG and [Keyword]'s own, so a card that built
    // them the other way round still prints the sentence Oracle prints.
    "colour and keyword sets print in the order Oracle prints them" {
        val reversed = CardFragment(
            script = CardScript(
                spellEffect = Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.RED, Color.BLUE),
                    creatureTypes = setOf("Otter"),
                    keywords = setOf(Keyword.TRAMPLE, Keyword.FLYING),
                )
            )
        )
        Grammar.abilityLine.printLine(reversed) shouldBe
            "Create a 2/2 blue and red Otter creature token with flying and trample."
    }

    // …and WUBRG is a *cycle* for a pair: the run starts at whichever colour leaves the other within
    // two steps forward, which is the adjacency Magic names its allied and enemy pairs by. A plain
    // ordinal sort spells five of the ten backwards; Exhibition Magician is the card that said so.
    "a two-colour run starts where Oracle starts it, not at W" {
        roundTrips("Create a 1/1 green and white Citizen creature token.")
        roundTrips("Create a 2/2 black and green Zombie creature token.")
        roundTrips("Create a 1/1 white and blue Bird creature token with flying.")
        roundTrips("Create a 2/2 red and white Dwarf creature token.")

        val stored = CardFragment(
            script = CardScript(
                spellEffect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE, Color.GREEN),
                    creatureTypes = setOf("Citizen"),
                )
            )
        )
        Grammar.abilityLine.printLine(stored) shouldBe "Create a 1/1 green and white Citizen creature token."
    }

    "a predefined token is the noun the SDK names" {
        fragment("Create a Food token.").script.spellEffect shouldBe Effects.CreateFood()
        fragment("Create two Treasure tokens.").script.spellEffect shouldBe Effects.CreateTreasure(count = 2)

        roundTrips("Create a Food token.")
        roundTrips("Create a Treasure token.")
        roundTrips("Create two Treasure tokens.")
        roundTrips("Create three Clue tokens.")
    }

    // The two token effects are different SDK types, so neither may print the other's sentence.
    "a spelled-out token and a predefined one do not print each other" {
        val food = CardFragment(script = CardScript(spellEffect = CreatePredefinedTokenEffect("Food")))
        Grammar.abilityLine.printLine(food) shouldBe "Create a Food token."

        // A predefined noun the vocabulary does not carry has no sentence, rather than a wrong one.
        val unknown = CardFragment(script = CardScript(spellEffect = CreatePredefinedTokenEffect("Junk")))
        Grammar.abilityLine.printLine(unknown) shouldBe null
    }

    // CR 701.16a: investigating creates a Clue, but it is its own model — "whenever you
    // investigate" watches the action, which a plain "create a Clue token" is not.
    "investigate is its own model, distinct from creating a Clue" {
        fragment("Investigate.").script.spellEffect shouldBe Effects.Investigate()
        fragment("Investigate.").script.spellEffect shouldNotBe Effects.CreateClue()
        fragment("When ~ enters, investigate twice.").script.triggeredAbilities.single().effect shouldBe
            Effects.Investigate(2)

        roundTrips("Investigate.")
        roundTrips("When ~ enters, investigate twice.")
        roundTrips("Create a Clue token.")
    }

    "the token clause is the same clause inside a trigger" {
        roundTrips("When ~ enters, create a Food token.")
        roundTrips("When ~ enters, create two 1/1 white Rabbit creature tokens.")
    }

    // "That many" names the quantity the sentence before it reported, and the corpus prints it after
    // six different antecedents. So the clause is registered only where the antecedent is known — a
    // damage trigger's payoff — and the count is the damage that trigger carries.
    "the that-many count is readable after a damage trigger and nowhere else" {
        fragment("Whenever ~ deals damage, create that many Blood tokens.")
            .script.triggeredAbilities.single().effect shouldBe Effects.CreateBlood(
            DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)
        )
        roundTrips("Whenever ~ deals damage, create that many Blood tokens.")
        roundTrips("Whenever ~ deals combat damage to a player, create that many Treasure tokens.")
        roundTrips(
            "Whenever ~ deals combat damage to a player, create that many 1/1 green Saproling " +
                "creature tokens."
        )
        // The same words after a trigger that reports no damage have no antecedent this grammar can
        // read, so they decline rather than being read as the damage amount.
        Grammar.abilityLine.parseLine("When ~ enters, create that many Blood tokens.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Whenever ~ attacks, create that many Treasure tokens.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
